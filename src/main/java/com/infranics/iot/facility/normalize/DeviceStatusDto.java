package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

import java.time.Instant;
import java.util.Map;

/**
 * MQTT에서 수신한 원본 JSON을 장비별 정규화 로직을 거쳐 표준화한 결과.
 * WebSocket으로는 rawPayload를 절대 그대로 내보내지 않고 이 DTO를 사용한다.
 */
public record DeviceStatusDto(
        String apiKey,
        String sensorNetworkUid,
        DeviceType deviceType,
        String cmd,
        Instant receivedAt,
        Map<String, Object> normalizedFields,
        JsonNode rawPayload
) {
}
