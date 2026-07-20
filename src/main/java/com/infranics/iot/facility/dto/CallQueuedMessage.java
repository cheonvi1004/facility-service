package com.infranics.iot.facility.dto;

import com.infranics.iot.facility.device.DeviceType;

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
