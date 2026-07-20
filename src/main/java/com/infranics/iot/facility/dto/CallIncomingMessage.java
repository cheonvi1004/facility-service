package com.infranics.iot.facility.dto;

import java.time.Instant;

import com.infranics.iot.facility.device.DeviceType;

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
