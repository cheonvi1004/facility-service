package com.infranics.iot.facility.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infranics.iot.facility.dto.OutboundMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 연결된 모든 web 클라이언트에게 메시지를 발송.
 *
 * broadcast()와 broadcastImmediate()를 분리해둔 이유:
 * 지금은 둘 다 즉시 동기 발송이지만, 추후 일반 상태 업데이트에 발송 큐/배치를 도입하더라도
 * 화재·통화 요청 같은 긴급 이벤트(Immediate)는 큐를 우회해야 하므로 호출부의 의도를 명확히 구분해둔다.
 */
@Component
public class DeviceWebSocketBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(DeviceWebSocketBroadcaster.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public DeviceWebSocketBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    /** 일반 우선순위 상태 업데이트 */
    public void broadcast(OutboundMessage message) {
        send(message);
    }

    /** 화재/통화 요청 등 긴급 이벤트 - 즉시 발송 */
    public void broadcastImmediate(OutboundMessage message) {
        send(message);
    }

    public void sendTo(WebSocketSession session, OutboundMessage message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private void send(OutboundMessage message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize outbound message {}", message.type(), e);
            return;
        }
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
