package com.infranics.iot.facility.dto;

public record CallEndedMessage(
        String callId,
        String reason
) implements OutboundMessage {

    @Override
    public String type() {
        return "CALL_ENDED";
    }
}
