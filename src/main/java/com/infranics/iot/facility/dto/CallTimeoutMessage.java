package com.infranics.iot.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CallTimeoutMessage(
        String callId
) implements OutboundMessage {

	@JsonProperty("type")
    @Override
    public String type() {
        return "CALL_TIMEOUT";
    }
}
