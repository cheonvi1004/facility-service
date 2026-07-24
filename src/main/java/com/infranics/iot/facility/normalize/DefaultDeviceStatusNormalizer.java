package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infranics.iot.facility.device.DeviceType;

import java.time.Instant;
import java.util.Map;

/**
 * 장비별 전용 Normalizer가 없을 때 쓰는 fallback.
 * "Data" 필드를 그대로 Map으로 변환해서 넘긴다 (필드명 표준화는 하지 않음).
 * 실제 운영 전 각 장비별 전용 Normalizer(LightStatusNormalizer 등)로 교체 권장.
 */
public class DefaultDeviceStatusNormalizer implements DeviceStatusNormalizer {

    private final DeviceType deviceType;
    private final ObjectMapper objectMapper;

    public DefaultDeviceStatusNormalizer(DeviceType deviceType, ObjectMapper objectMapper) {
        this.deviceType = deviceType;
        this.objectMapper = objectMapper;
    }

    @Override
    public DeviceType supports() {
        return deviceType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        JsonNode dataNode = rawPayload.path("Data");
        Map<String, Object> fields;
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            fields = Map.of();
        } else if (dataNode.isObject()) {
            fields = objectMapper.convertValue(dataNode, Map.class);
        } else {
            // 문자열/배열 등 단순 값인 경우 "value" 키로 감싼다 (예: "Data": "0x01")
            fields = Map.of("value", objectMapper.convertValue(dataNode, Object.class));
        }

        return new DeviceStatusDto(
                apiKey,
                sensorNetworkUid,
                deviceType,
                cmd,
                Instant.now(),
                fields,
                rawPayload
        );
    }
}
