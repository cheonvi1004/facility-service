package com.infranics.iot.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CallEndedMessage(
        String callId,
        String reason
) implements OutboundMessage {

	@JsonProperty("type")
    @Override
    public String type() {
        return "CALL_ENDED";
    }
}
