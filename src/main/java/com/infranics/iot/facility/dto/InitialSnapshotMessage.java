package com.infranics.iot.facility.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InitialSnapshotMessage(
        List<StatusUpdateMessage> devices
) implements OutboundMessage {

	@JsonProperty("type")
    @Override
    public String type() {
        return "INITIAL_SNAPSHOT";
    }
}
