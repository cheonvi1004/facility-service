package com.infranics.iot.facility.dto;

import java.util.List;

public record InitialSnapshotMessage(
        List<StatusUpdateMessage> devices
) implements OutboundMessage {

    @Override
    public String type() {
        return "INITIAL_SNAPSHOT";
    }
}
