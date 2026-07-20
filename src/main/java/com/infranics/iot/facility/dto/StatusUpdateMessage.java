package com.infranics.iot.facility.dto;

import java.time.Instant;
import java.util.Map;

import com.infranics.iot.facility.device.DeviceType;

public record StatusUpdateMessage(
        DeviceType deviceType,
        String sensorNetworkUid,
        Instant timestamp,
        Map<String, Object> fields
) implements OutboundMessage {

    @Override
    public String type() {
        return "STATUS_UPDATE";
    }
}
