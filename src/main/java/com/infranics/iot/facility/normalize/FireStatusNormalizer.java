package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 문서 4.3절 0xA2(주기적 상태 정보 전송) 기준.
 * FireInfos[] 배열 중 하나라도 fire_state=0x01이면 전체 fireState를 "0x01"로 종합한다.
 * (DeviceMessageDispatcher의 우선순위 판단이 이 필드를 기준으로 동작함)
 */
@Component
public class FireStatusNormalizer implements DeviceStatusNormalizer {

	private static final Map<String, String> DOOR_STATE_MAP = Map.of(
		    "0x00", "닫힘",
		    "0x01", "열림"
		);

		private static final Map<String, String> RAMP_STATE_MAP = Map.of(
		    "0x00", "OFF",
		    "0x01", "ON"
		);
		
    @Override
    public DeviceType supports() {
        return DeviceType.FIRE;
    }

    @Override
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        JsonNode data = rawPayload.path("Data");
        Map<String, Object> fields = new HashMap<>();

        fields.put("rampState",
                RAMP_STATE_MAP.getOrDefault(textOrNull(data, "ramp_state"), "UNKNOWN"));

        boolean anyZoneFire = false;
        List<Map<String, Object>> zones = new ArrayList<>();
        for (JsonNode zone : data.path("FireInfos")) {
            String zoneId = textOrNull(zone, "zone_id");
            String fireState = textOrNull(zone, "fire_state");
            if ("0x01".equalsIgnoreCase(fireState)) {
                anyZoneFire = true;
            }
            Map<String, Object> zoneMap = new HashMap<>();
            zoneMap.put("zoneId", zoneId);
            zoneMap.put("fireState", fireState);
            //zoneMap.put("doorState", zone.path("door_state"));
            List<String> doorStates = new ArrayList<>();

            for (JsonNode door : zone.path("door_state")) {
                doorStates.add(
                    DOOR_STATE_MAP.getOrDefault(door.asText(), door.asText())
                );
            }

            zoneMap.put("doorState", doorStates);

            zones.add(zoneMap);
            zones.add(zoneMap);
        }

        fields.put("fireState", anyZoneFire ? "화재" : "일반");
        fields.put("zones", zones);

        return new DeviceStatusDto(apiKey, sensorNetworkUid, DeviceType.FIRE, cmd, Instant.now(), fields, rawPayload);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
    
}
