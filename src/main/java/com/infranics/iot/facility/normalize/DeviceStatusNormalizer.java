package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

/**
 * 장비종류별 원본 JSON -> 표준 DeviceStatusDto 변환.
 * 문서상 필드명 표기가 장비마다 제각각(pump1_state vs pump2_State 등)이므로
 * 장비별 구현체에서 필드명/타입을 일관되게 맞춘다.
 */
public interface DeviceStatusNormalizer {

    DeviceType supports();

    DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload);
}
