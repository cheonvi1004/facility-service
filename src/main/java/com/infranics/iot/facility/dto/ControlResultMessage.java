package com.infranics.iot.facility.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ControlResultMessage(
        String requestId,
        String sensorNetworkUid,
        Result result,
        Instant respondedAt
) implements OutboundMessage {

    public enum Result { SUCCESS, TIMEOUT, ERROR }

    @JsonProperty("type")
    @Override
    public String type() {
        return "CONTROL_RESULT";
    }
}
