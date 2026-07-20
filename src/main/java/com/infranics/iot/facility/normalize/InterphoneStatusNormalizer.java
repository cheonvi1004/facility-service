package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class InterphoneStatusNormalizer implements DeviceStatusNormalizer {

    @Override
    public DeviceType supports() {
        return DeviceType.INTERPHONE;
    }

    @Override
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        Map<String, Object> fields = new HashMap<>();

        // 인터폰은 Data 블록이 없는 경우가 많고 Cmd 자체가 현재 상태/이벤트를 대변합니다.
        String callEvent = switch (cmd.toUpperCase()) {
            case "0XA1" -> "CALL_REQUESTED";
            case "0XA2" -> "CALL_ENDED";
            case "0XA3" -> "CALL_CANCELED";
            case "0XA4" -> "CALL_ACCEPTED";
            case "0XA5" -> "CALL_REJECTED_TIMEOUT";
            case "0XA6", "0X06" -> "PING_PONG";
            default -> "UNKNOWN";
        };
        
        fields.put("callEvent", callEvent);

        return new DeviceStatusDto(apiKey, sensorNetworkUid, DeviceType.INTERPHONE, cmd, Instant.now(), fields, rawPayload);
    }
}