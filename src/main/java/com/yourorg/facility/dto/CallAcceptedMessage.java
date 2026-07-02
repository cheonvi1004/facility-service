package com.yourorg.facility.dto;

public record CallAcceptedMessage(
        String callId,
        String sensorNetworkUid
) implements OutboundMessage {

    @Override
    public String type() {
        return "CALL_ACCEPTED";
    }
}
