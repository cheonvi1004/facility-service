package com.infranics.iot.facility.dto;

public record CallTimeoutMessage(
        String callId
) implements OutboundMessage {

    @Override
    public String type() {
        return "CALL_TIMEOUT";
    }
}
