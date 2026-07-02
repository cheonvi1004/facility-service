package com.yourorg.facility.dto;

import com.yourorg.facility.device.DeviceType;

public record CallQueuedMessage(
        String callId,
        DeviceType deviceType,
        String sensorNetworkUid
) implements OutboundMessage {

    @Override
    public String type() {
        return "CALL_QUEUED";
    }
}
