package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 문서 4.5절 배수펌프 0xA9 응답 기준.
 * 원본 필드명 표기가 혼재됨(pump1_state / pump2_State 등) -> 표준 camelCase로 통일.
 */
@Component
public class PumpStatusNormalizer implements DeviceStatusNormalizer {
	
	private static final Map<String, String> MODE_MAP = Map.of(
		    "0x00", "자동",
		    "0x01", "수동"
		);

		private static final Map<String, String> LEAK_STATE_MAP = Map.of(
		    "0x00", "미감지",
		    "0x01", "감지"
		);
		
		private static final Map<String, String> PUMP_STATE_MAP = Map.of(
			    "0x00", "OFF",
			    "0x01", "ON"
			);
		
		private static final Map<String, String> WL_LV_STATE_MAP = Map.of(
			    "0x00", "LL",
			    "0x01", "PL",
			    "0x02", "PH",
			    "0x03", "HH"
			);
		
		private static final Map<String, String> SENSOR_STATE_MAP = Map.of(
			    "0x00", "정상",
			    "0x01", "고장"
			);


    @Override
    public DeviceType supports() {
        return DeviceType.PUMP;
    }

    @Override
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        JsonNode data = rawPayload.path("Data");
        Map<String, Object> fields = new HashMap<>();
        
//        {"APIKEY":"0001-2606-0301","SensorNetworkUID":"0001-0001-0007-0001","SensorName":"pump","Cmd":"0xA9",
//        	 "Data":{"Mode":"0x01","Leak1_state":"","Leak2_state":"","pump1_State":"0x00","pump2_State":"0x00",
//        	 "wl_lv_state":"LL","sensor_state":"0x01","wl_value":"0","motor1_temp":"","bearing1_temp":"",
//        	 "motor2_temp":"","bearing2_temp":"","tank_height":"1000","LL_Lv_mm":"200","PL_Lv_mm":"300",
//        	 "PH_Lv_mm":"500","HH_Lv_mm":"700"}}

        fields.put("mode",  MODE_MAP.getOrDefault(textOrNull(data, "Mode"), "UNKNOWN"));
        fields.put("Leak1_state", LEAK_STATE_MAP.getOrDefault(textOrNull(data, "Leak1_state"), ""));
        fields.put("Leak2_state", LEAK_STATE_MAP.getOrDefault(textOrNull(data, "Leak2_state"), ""));
        fields.put("pump1_state", PUMP_STATE_MAP.getOrDefault(firstNonNull(data, "pump1_state", "pump1_State"), ""));
        fields.put("pump2_state", PUMP_STATE_MAP.getOrDefault(firstNonNull(data, "pump2_state", "pump2_State", "pump2State"), "")); 
        fields.put("waterLevelState", WL_LV_STATE_MAP.getOrDefault(textOrNull(data, "wl_lv_state"), ""));
        fields.put("sensor_state",  SENSOR_STATE_MAP.getOrDefault(textOrNull(data, "sensor_state"), ""));
        fields.put("waterLevelMm", numberOrNull(data, "wl_value"));
        fields.put("motor1TempC", numberOrNull(data, "motor1_temp"));
        fields.put("bearing1TempC", numberOrNull(data, "bearing1_temp"));
        fields.put("motor2TempC", numberOrNull(data, "motor2_temp"));
        fields.put("bearing2TempC", numberOrNull(data, "bearing2_temp"));
        fields.put("tankHeightMm", numberOrNull(data, "tank_height"));
        fields.put("LL_Lv_mm", numberOrNull(data, "LL_Lv_mm"));
        fields.put("PL_Lv_mm", numberOrNull(data, "PL_Lv_mm"));
        fields.put("PH_Lv_mm", numberOrNull(data, "PH_Lv_mm"));
        fields.put("HH_Lv_mm", numberOrNull(data, "HH_Lv_mm"));

        return new DeviceStatusDto(apiKey, sensorNetworkUid, DeviceType.PUMP, cmd, Instant.now(), fields, rawPayload);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private String firstNonNull(JsonNode node, String... fieldCandidates) {
        for (String f : fieldCandidates) {
            String v = textOrNull(node, f);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private Double numberOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            return Double.parseDouble(v.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
