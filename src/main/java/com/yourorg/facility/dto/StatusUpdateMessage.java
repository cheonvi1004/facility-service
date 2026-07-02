package com.yourorg.facility.dto;

import com.yourorg.facility.device.DeviceType;

import java.time.Instant;
import java.util.Map;

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
