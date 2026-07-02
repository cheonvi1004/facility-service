package com.yourorg.facility.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.facility.call.CallSessionManager;
import com.yourorg.facility.device.DeviceControlService;
import com.yourorg.facility.dto.CallResponseMessage;
import com.yourorg.facility.dto.ControlRequestMessage;
import com.yourorg.facility.dto.InitialSnapshotMessage;
import com.yourorg.facility.dto.StatusUpdateMessage;
import com.yourorg.facility.normalize.CurrentStatusStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

/**
 * web <-> svc WebSocket 엔드포인트.
 * 연결 직후 INITIAL_SNAPSHOT을 전달하고, 이후 CONTROL_REQUEST / CALL_RESPONSE 인바운드 메시지를 라우팅한다.
 *
 * 주의: 이 핸들러는 권한 체크를 하지 않는다. 실제 운영에서는
 * afterConnectionEstablished 시점(JWT 등)뿐 아니라 각 인바운드 메시지마다
 * 권한을 재검증하는 로직을 추가해야 한다 (세션 중간 권한 변경 대응).
 */
@Component
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceWebSocketHandler.class);

    private final DeviceWebSocketBroadcaster broadcaster;
    private final CurrentStatusStore statusStore;
    private final DeviceControlService controlService;
    private final CallSessionManager callSessionManager;
    private final ObjectMapper objectMapper;

    public DeviceWebSocketHandler(DeviceWebSocketBroadcaster broadcaster,
                                   CurrentStatusStore statusStore,
                                   DeviceControlService controlService,
                                   CallSessionManager callSessionManager,
                                   ObjectMapper objectMapper) {
        this.broadcaster = broadcaster;
        this.statusStore = statusStore;
        this.controlService = controlService;
        this.callSessionManager = callSessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.register(session);

        List<StatusUpdateMessage> snapshot = statusStore.snapshot().stream()
                .map(s -> new StatusUpdateMessage(s.deviceType(), s.sensorNetworkUid(), s.receivedAt(), s.normalizedFields()))
                .toList();
        broadcaster.sendTo(session, new InitialSnapshotMessage(snapshot));

        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.unregister(session);
        log.info("WebSocket closed: {} ({})", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText(null);

            switch (type) {
                case "CONTROL_REQUEST" -> {
                    ControlRequestMessage request = objectMapper.treeToValue(root, ControlRequestMessage.class);
                    controlService.handle(request);
                }
                case "CALL_RESPONSE" -> {
                    CallResponseMessage response = objectMapper.treeToValue(root, CallResponseMessage.class);
                    callSessionManager.onOperatorResponse(response.callId(),
                            response.action() == CallResponseMessage.Action.ACCEPT);
                }
                default -> log.warn("Unknown inbound message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to handle inbound WebSocket message: {}", e.getMessage());
        }
    }
}
