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

	private static final Map<String, String> LOUVERS_STATE_MAP = Map.of(
		    "0x00", "닫힘",
		    "0x01", "열림"
		);
	
    @Override
    public DeviceType supports() {
        return DeviceType.LOUVER;
    }

    @Override
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        JsonNode data = rawPayload.path("Data");
        Map<String, Object> fields = new HashMap<>();

        if ("0xA9".equalsIgnoreCase(cmd)) {
            List<Map<String, Object>> louverInfos = new ArrayList<>();
            JsonNode infoArray = data.path("louvers_Infos");
            
            if (infoArray.isArray()) {
                for (JsonNode node : infoArray) {
                    Map<String, Object> state = new HashMap<>();
                    state.put("humidity", node.path("hum").asText());
                    state.put("temperature", node.path("temp").asText());
                    state.put("isOpen", "1".equals(node.path("isopen").asText()) || "0x01".equals(node.path("isopen").asText()));
                    louverInfos.add(state);
                }
            }
            fields.put("louverInfos", louverInfos);
            fields.put("louverStates",  LOUVERS_STATE_MAP.getOrDefault(textOrNull(data, "louvers_state"), ""));
        }

        return new DeviceStatusDto(apiKey, sensorNetworkUid, DeviceType.LOUVER, cmd, Instant.now(), fields, rawPayload);
    }
    
    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}