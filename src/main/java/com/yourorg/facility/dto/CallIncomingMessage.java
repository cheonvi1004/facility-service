package com.yourorg.facility.dto;

import com.yourorg.facility.device.DeviceType;

import java.time.Instant;

public record CallIncomingMessage(
        String callId,
        DeviceType deviceType,
        String sensorNetworkUid,
        Instant requestedAt
) implements OutboundMessage {

    @Override
    public String type() {
        return "CALL_INCOMING";
    }
}
