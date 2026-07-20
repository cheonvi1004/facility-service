package com.infranics.iot.facility.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.call.CallSessionManager;
import com.infranics.iot.facility.correlation.PendingCommandTracker;
import com.infranics.iot.facility.device.DeviceType;
import com.infranics.iot.facility.dto.ControlResultMessage;
import com.infranics.iot.facility.dto.StatusUpdateMessage;
import com.infranics.iot.facility.entity.DeviceCurrentStatus;
import com.infranics.iot.facility.entity.DeviceStatusLog;
import com.infranics.iot.facility.normalize.CurrentStatusStore;
import com.infranics.iot.facility.normalize.DeviceStatusDto;
import com.infranics.iot.facility.normalize.DeviceStatusNormalizerRegistry;
import com.infranics.iot.facility.repository.DeviceCurrentStatusRepository;
import com.infranics.iot.facility.repository.DeviceStatusLogRepository;
import com.infranics.iot.facility.ws.DeviceWebSocketBroadcaster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

/**
 * MqttGateway가 발행한 이벤트를 받아 실제 처리를 담당하는 총괄 라우터.
 * - 요청-응답 상관관계 해소 (PendingCommandTracker)
 * - 통화 관련 Cmd는 CallSessionManager로 위임
 * - 그 외 상태값은 정규화 후 WebSocket으로 브로드캐스트 (화재는 즉시 발송)
 */
@Component
public class DeviceMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DeviceMessageDispatcher.class);

    // 문서 4.6/4.7 기준 통화 요청 관련 Cmd (장치 -> 미들웨어)
    private static final Set<String> INTERPHONE_CALL_REQUEST_CMDS = Set.of("0x02", "0xA1");
    private static final Set<String> ACCESS_CALL_REQUEST_CMDS = Set.of("0x02");
    private static final String INTERPHONE_AUTO_TIMEOUT_CMD = "0xA5";
    private static final String INTERPHONE_CALL_ENDED_CMD = "0xA2";

    private final PendingCommandTracker pendingCommandTracker;
    private final DeviceStatusNormalizerRegistry normalizerRegistry;
    private final CurrentStatusStore statusStore;
    private final DeviceWebSocketBroadcaster broadcaster;
    private final CallSessionManager callSessionManager;
    private final DeviceCurrentStatusRepository currentStatusRepository;
    private final DeviceStatusLogRepository statusLogRepository;
    
    
    public DeviceMessageDispatcher(PendingCommandTracker pendingCommandTracker,
                                    DeviceStatusNormalizerRegistry normalizerRegistry,
                                    CurrentStatusStore statusStore,
                                    DeviceWebSocketBroadcaster broadcaster,
                                    CallSessionManager callSessionManager,
                                    DeviceCurrentStatusRepository currentStatusRepository,
                                    DeviceStatusLogRepository statusLogRepository
                                    ) {
        this.pendingCommandTracker = pendingCommandTracker;
        this.normalizerRegistry = normalizerRegistry;
        this.statusStore = statusStore;
        this.broadcaster = broadcaster;
        this.callSessionManager = callSessionManager;
        this.currentStatusRepository = currentStatusRepository;
        this.statusLogRepository = statusLogRepository;
        
    }

    @EventListener
    public void onMqttMessage(MqttMessageReceivedEvent event) {
        JsonNode payload = event.payload();
        String cmd = payload.path("Cmd").asText(null);
        String sensorNetworkUid = payload.path("SensorNetworkUID").asText(null);
        DeviceType deviceType = event.deviceType();
        String apiKey = event.apiKey();

        if (sensorNetworkUid == null) {
            log.warn("Message without SensorNetworkUID, ignored: deviceType={} payload={}", deviceType, payload);
            return;
        }

        // 1. 제어 요청에 대한 응답인지 확인
        pendingCommandTracker.resolve(sensorNetworkUid, cmd).ifPresent(pending ->
                broadcaster.broadcast(new ControlResultMessage(
                        pending.requestId(), sensorNetworkUid, ControlResultMessage.Result.SUCCESS, Instant.now())));

        // 2. 통화 관련 Cmd는 CallSessionManager로 위임
        if (isCallRequest(deviceType, cmd)) {
            callSessionManager.onIncomingCall(apiKey, deviceType, sensorNetworkUid);
            return;
        }
        if (deviceType == DeviceType.INTERPHONE && INTERPHONE_AUTO_TIMEOUT_CMD.equalsIgnoreCase(cmd)) {
            callSessionManager.onDeviceReportedTimeout(sensorNetworkUid);
            return;
        }
        if (deviceType == DeviceType.INTERPHONE && INTERPHONE_CALL_ENDED_CMD.equalsIgnoreCase(cmd)) {
            callSessionManager.onCallEndedByDevice(sensorNetworkUid);
            return;
        }

        // 3. 일반 상태값 -> 정규화 후 브로드캐스트
        DeviceStatusDto status = normalizerRegistry.get(deviceType)
                .normalize(apiKey, sensorNetworkUid, cmd, payload);
        
     // 3-1. 인메모리 업데이트
        statusStore.update(status);
        
        // 3-2. RDB 최신 상태 테이블 저장 (UPSERT 처리)
        DeviceCurrentStatus currentStatusEntity = DeviceCurrentStatus.builder()
                .sensorNetworkUid(status.sensorNetworkUid())
                .apiKey(status.apiKey())
                .deviceType(status.deviceType())
                .cmd(status.cmd())
                .receivedAt(status.receivedAt().atOffset(ZoneOffset.UTC))
                .normalizedFields(status.normalizedFields())
                .rawPayload(status.rawPayload())
                .build();
        currentStatusRepository.save(currentStatusEntity); // PK가 UID이므로 자동 UPSERT 됨

        // 3-3. RDB 누적 로그 테이블 저장 (이력 보관용)
        DeviceStatusLog logEntity = DeviceStatusLog.builder()
                .sensorNetworkUid(status.sensorNetworkUid())
                .apiKey(status.apiKey())
                .deviceType(status.deviceType())
                .cmd(status.cmd())
                .receivedAt(status.receivedAt().atOffset(ZoneOffset.UTC))
                .normalizedFields(status.normalizedFields())
                .rawPayload(status.rawPayload())
                .build();
        statusLogRepository.save(logEntity);

        StatusUpdateMessage message = new StatusUpdateMessage(
                deviceType, sensorNetworkUid, status.receivedAt(), status.normalizedFields());

        if (isFireDetected(deviceType, status)) {
            broadcaster.broadcastImmediate(message);
        } else {
            broadcaster.broadcast(message);
        }
    }

    private boolean isCallRequest(DeviceType deviceType, String cmd) {
        if (cmd == null) {
            return false;
        }
        return switch (deviceType) {
            case INTERPHONE -> INTERPHONE_CALL_REQUEST_CMDS.contains(cmd.toUpperCase());
            case ACCESS -> ACCESS_CALL_REQUEST_CMDS.contains(cmd.toUpperCase());
            default -> false;
        };
    }

    private boolean isFireDetected(DeviceType deviceType, DeviceStatusDto status) {
        if (deviceType != DeviceType.FIRE) {
            return false;
        }
        Object fireState = status.normalizedFields().get("fireState");
        return "0x01".equalsIgnoreCase(String.valueOf(fireState));
    }
}
