package com.yourorg.facility.dto;

/**
 * svc -> web 으로 나가는 모든 WebSocket 메시지가 구현하는 마커 인터페이스.
 * Jackson 직렬화 시 각 record의 type 필드가 그대로 JSON에 포함된다.
 */
public sealed interface OutboundMessage
        permits StatusUpdateMessage, InitialSnapshotMessage, ControlResultMessage,
        CallIncomingMessage, CallQueuedMessage, CallTimeoutMessage, CallAcceptedMessage, CallEndedMessage {

    String type();
}
