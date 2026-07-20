package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LouverStatusNormalizer implements DeviceStatusNormalizer {

    @Override
    public DeviceType supports() {
        return DeviceType.LOUVER;
    }

    @Override
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        JsonNode data = rawPayload.path("Data");
        Map<String, Object> fields = new HashMap<>();

        if ("0xA9".equalsIgnoreCase(cmd)) {
            List<Map<String, Object>> louverStates = new ArrayList<>();
            JsonNode statesArray = data.path("louvers_state");
            
            if (statesArray.isArray()) {
                for (JsonNode node : statesArray) {
                    Map<String, Object> state = new HashMap<>();
                    state.put("humidity", node.path("hum").asText());
                    state.put("temperature", node.path("temp").asText());
                    state.put("isOpen", "1".equals(node.path("isopen").asText()) || "0x01".equals(node.path("isopen").asText()));
                    louverStates.add(state);
                }
            }
            fields.put("louverStates", louverStates);
        }

        return new DeviceStatusDto(apiKey, sensorNetworkUid, DeviceType.LOUVER, cmd, Instant.now(), fields, rawPayload);
    }
}