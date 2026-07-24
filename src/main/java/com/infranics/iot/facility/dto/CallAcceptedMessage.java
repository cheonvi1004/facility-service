package com.infranics.iot.facility.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CallAcceptedMessage(
        String callId,
        String sensorNetworkUid
) implements OutboundMessage {

	@JsonProperty("type")
    @Override
    public String type() {
        return "CALL_ACCEPTED";
    }
}
