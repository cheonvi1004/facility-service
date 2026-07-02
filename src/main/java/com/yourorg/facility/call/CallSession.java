package com.yourorg.facility.call;

import com.yourorg.facility.device.DeviceType;

import java.time.Instant;

public class CallSession {

    private final String callId;
    private final String apiKey;
    private final String sensorNetworkUid;
    private final DeviceType deviceType;
    private final Instant requestedAt;

    private volatile CallSessionState state;
    private volatile Instant respondedAt;

    public CallSession(String callId, String apiKey, String sensorNetworkUid, DeviceType deviceType) {
        this.callId = callId;
        this.apiKey = apiKey;
        this.sensorNetworkUid = sensorNetworkUid;
        this.deviceType = deviceType;
        this.requestedAt = Instant.now();
        this.state = CallSessionState.RINGING;
    }

    public void transitionTo(CallSessionState next) {
        this.state = next;
        if (next == CallSessionState.ACCEPTED || next == CallSessionState.REJECTED) {
            this.respondedAt = Instant.now();
        }
    }

    public String callId() { return callId; }
    public String apiKey() { return apiKey; }
    public String sensorNetworkUid() { return sensorNetworkUid; }
    public DeviceType deviceType() { return deviceType; }
    public CallSessionState state() { return state; }
    public Instant requestedAt() { return requestedAt; }
    public Instant respondedAt() { return respondedAt; }
}
