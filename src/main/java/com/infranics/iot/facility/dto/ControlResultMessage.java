package com.infranics.iot.facility.dto;

import java.time.Instant;

public record ControlResultMessage(
        String requestId,
        String sensorNetworkUid,
        Result result,
        Instant respondedAt
) implements OutboundMessage {

    public enum Result { SUCCESS, TIMEOUT, ERROR }

    @Override
    public String type() {
        return "CONTROL_RESULT";
    }
}
