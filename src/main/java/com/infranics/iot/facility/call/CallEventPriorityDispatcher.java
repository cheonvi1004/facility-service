package com.infranics.iot.facility.call;

import org.springframework.stereotype.Component;

import com.infranics.iot.facility.dto.*;
import com.infranics.iot.facility.ws.DeviceWebSocketBroadcaster;

/**
 * 통화 요청/타임아웃/수락은 화재 알람과 동급의 긴급 이벤트로 취급하여
 * 일반 상태 업데이트보다 우선(즉시) 발송한다.
 */
@Component
public class CallEventPriorityDispatcher {

    private final DeviceWebSocketBroadcaster broadcaster;

    public CallEventPriorityDispatcher(DeviceWebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void dispatchIncoming(CallSession session) {
        broadcaster.broadcastImmediate(new CallIncomingMessage(
                session.callId(), session.deviceType(), session.sensorNetworkUid(), session.requestedAt()));
    }

    public void dispatchQueued(CallSession session) {
        broadcaster.broadcast(new CallQueuedMessage(
                session.callId(), session.deviceType(), session.sensorNetworkUid()));
    }

    public void dispatchTimeout(CallSession session) {
        broadcaster.broadcastImmediate(new CallTimeoutMessage(session.callId()));
    }

    public void dispatchAccepted(CallSession session) {
        broadcaster.broadcastImmediate(new CallAcceptedMessage(session.callId(), session.sensorNetworkUid()));
    }

    public void dispatchEnded(CallSession session, String reason) {
        broadcaster.broadcast(new CallEndedMessage(session.callId(), reason));
    }
}
