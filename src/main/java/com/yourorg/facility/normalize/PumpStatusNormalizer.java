package com.yourorg.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.yourorg.facility.device.DeviceType;
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

    @Override
    public DeviceType supports() {
        return DeviceType.PUMP;
    }

    @Override
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        JsonNode data = rawPayload.path("Data");
        Map<String, Object> fields = new HashMap<>();

        fields.put("mode", textOrNull(data, "Mode"));
        fields.put("leakDetected", "0x01".equalsIgnoreCase(textOrNull(data, "Leak_state")));
        fields.put("pump1On", "0x01".equalsIgnoreCase(firstNonNull(data, "pump1_state", "pump1State")));
        fields.put("pump2On", "0x01".equalsIgnoreCase(firstNonNull(data, "pump2_state", "pump2_State", "pump2State")));
        fields.put("waterLevelState", textOrNull(data, "wl_lv_state"));
        fields.put("sensorFault", "0x01".equalsIgnoreCase(textOrNull(data, "sensor_state")));
        fields.put("waterLevelMm", numberOrNull(data, "wl_value"));
        fields.put("motorTempC", numberOrNull(data, "motor_temp"));
        fields.put("bearingTempC", numberOrNull(data, "bearing_temp"));
        fields.put("tankHeightMm", numberOrNull(data, "tank_height"));
        fields.put("thresholdLLmm", numberOrNull(data, "LL_Lv_mm"));
        fields.put("thresholdPLmm", numberOrNull(data, "PL_Lv_mm"));
        fields.put("thresholdPHmm", numberOrNull(data, "PH_Lv_mm"));
        fields.put("thresholdHHmm", numberOrNull(data, "HH_Lv_mm"));

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
