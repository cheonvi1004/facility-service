package com.infranics.iot.facility.call;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.infranics.iot.facility.config.FacilityCallProperties;
import com.infranics.iot.facility.device.DeviceType;
import com.infranics.iot.facility.mqtt.MqttGateway;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 문서 4.6(출입관리설비), 4.7(인터폰) 기준 통화 요청 흐름 관리.
 * deviceType 단위로 활성 세션 1개 제한 (관제요원 1인 기준).
 * 관제요원이 여러 명인 현장이라면 activeSessions 키를 그룹/요원 단위로 확장 필요.
 */
@Component
public class CallSessionManager {

    private static final Logger log = LoggerFactory.getLogger(CallSessionManager.class);

    private final Map<DeviceType, CallSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, CallSession> sessionsByCallId = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1,
            r -> {
                Thread t = new Thread(r, "call-session-timeout");
                t.setDaemon(true);
                return t;
            });

    private final MqttGateway mqttGateway;
    private final CallEventPriorityDispatcher dispatcher;
    private final FacilityCallProperties callProperties;

    public CallSessionManager(MqttGateway mqttGateway, CallEventPriorityDispatcher dispatcher,
                               FacilityCallProperties callProperties) {
        this.mqttGateway = mqttGateway;
        this.dispatcher = dispatcher;
        this.callProperties = callProperties;
    }

    /** 장치로부터 통화 요청 수신 (Interphone: 0x02/0xA1, Access: 0x02) */
    public void onIncomingCall(String apiKey, DeviceType deviceType, String sensorNetworkUid) {
        CallSession existingActive = activeSessions.get(deviceType);

        if (existingActive != null && existingActive.state() == CallSessionState.IN_CALL) {
            CallSession queued = new CallSession(UUID.randomUUID().toString(), apiKey, sensorNetworkUid, deviceType);
            queued.transitionTo(CallSessionState.QUEUED);
            sessionsByCallId.put(queued.callId(), queued);

            if (deviceType == DeviceType.ACCESS) {
                // 출입관리설비 0x0B: 통화 대기 통보
                mqttGateway.publishControl(apiKey, deviceType, sensorNetworkUid, "0x0B", Map.of("Data", "0x00"), null);
            }
            dispatcher.dispatchQueued(queued);
            log.info("Call queued: callId={} device={} uid={}", queued.callId(), deviceType, sensorNetworkUid);
            return;
        }

        CallSession session = new CallSession(UUID.randomUUID().toString(), apiKey, sensorNetworkUid, deviceType);
        activeSessions.put(deviceType, session);
        sessionsByCallId.put(session.callId(), session);

        // 출입관리설비는 문서상 자동 타임아웃 규정이 없어 svc가 자체 타이머를 설정한다.
        // 인터폰은 장비가 약 30초 무응답 시 0xA5(거절됨)를 자체 통보하므로 svc 타이머가 불필요하다.
        if (deviceType == DeviceType.ACCESS) {
            scheduleTimeout(session, Duration.ofSeconds(callProperties.accessTimeoutSeconds()));
        }

        dispatcher.dispatchIncoming(session);
        log.info("Call incoming: callId={} device={} uid={}", session.callId(), deviceType, sensorNetworkUid);
    }

    /** web -> svc: 관제요원 수락/거절 */
    public void onOperatorResponse(String callId, boolean accept) {
        CallSession session = sessionsByCallId.get(callId);
        if (session == null || session.state() != CallSessionState.RINGING) {
            log.warn("CALL_RESPONSE ignored - session not RINGING: callId={}", callId);
            return;
        }

        // Interphone: 0x04 수락 / 0x05 거절, Access: 0x03 (Data 0x00 수락 / 0x01 거절)
        String cmd = resolveResponseCmd(session.deviceType(), accept);
        Object data = session.deviceType() == DeviceType.ACCESS ? Map.of("Data", accept ? "0x00" : "0x01") : null;

        session.transitionTo(accept ? CallSessionState.ACCEPTED : CallSessionState.REJECTED);
        mqttGateway.publishControl(session.apiKey(), session.deviceType(), session.sensorNetworkUid(), cmd, data, null);

        if (accept) {
            session.transitionTo(CallSessionState.IN_CALL);
            dispatcher.dispatchAccepted(session); // web에서 이 시점에 WebRTC 협상 시작
        } else {
            endSession(session, "REJECTED_BY_OPERATOR");
        }
    }

    private String resolveResponseCmd(DeviceType deviceType, boolean accept) {
        return switch (deviceType) {
            case INTERPHONE -> accept ? "0x04" : "0x05";
            case ACCESS -> "0x03";
            default -> throw new IllegalStateException("Call flow not supported for " + deviceType);
        };
    }

    /** 인터폰 0xA5(자동 거절 통보) 수신 시 호출 */
    public void onDeviceReportedTimeout(String sensorNetworkUid) {
        activeSessions.values().stream()
                .filter(s -> s.sensorNetworkUid().equals(sensorNetworkUid) && s.state() == CallSessionState.RINGING)
                .findFirst()
                .ifPresent(session -> {
                    session.transitionTo(CallSessionState.TIMEOUT);
                    dispatcher.dispatchTimeout(session);
                    endSession(session, "DEVICE_TIMEOUT");
                });
    }

    /** 통화 종료 (인터폰 0xA2, 또는 명시적 종료) */
    public void onCallEndedByDevice(String sensorNetworkUid) {
        activeSessions.values().stream()
                .filter(s -> s.sensorNetworkUid().equals(sensorNetworkUid))
                .findFirst()
                .ifPresent(session -> endSession(session, "ENDED_BY_DEVICE"));
    }

    private void scheduleTimeout(CallSession session, Duration timeout) {
        timeoutScheduler.schedule(() -> {
            if (session.state() == CallSessionState.RINGING) {
                session.transitionTo(CallSessionState.TIMEOUT);
                dispatcher.dispatchTimeout(session);
                endSession(session, "AUTO_TIMEOUT");
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void endSession(CallSession session, String reason) {
        session.transitionTo(CallSessionState.ENDED);
        activeSessions.remove(session.deviceType(), session);
        dispatcher.dispatchEnded(session, reason);
        promoteNextQueued(session.deviceType());
        // TODO: 감사 로그 - callId/uid/requestedAt/respondedAt/reason을 RDB에 append-only 저장 권장
    }

    private void promoteNextQueued(DeviceType deviceType) {
        Optional<CallSession> next = sessionsByCallId.values().stream()
                .filter(s -> s.deviceType() == deviceType && s.state() == CallSessionState.QUEUED)
                .findFirst();

        next.ifPresent(session -> {
            session.transitionTo(CallSessionState.RINGING);
            activeSessions.put(deviceType, session);
            if (deviceType == DeviceType.ACCESS) {
                scheduleTimeout(session, Duration.ofSeconds(callProperties.accessTimeoutSeconds()));
            }
            dispatcher.dispatchIncoming(session);
        });
    }
}
