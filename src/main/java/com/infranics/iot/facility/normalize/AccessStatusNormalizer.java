package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class AccessStatusNormalizer implements DeviceStatusNormalizer {

    @Override
    public DeviceType supports() {
        return DeviceType.ACCESS;
    }

    @Override
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        JsonNode data = rawPayload.path("Data");
        Map<String, Object> fields = new HashMap<>();

        switch (cmd.toUpperCase()) {
            case "0X08" -> {
                fields.put("door01Open", "0x01".equalsIgnoreCase(data.path("door01").asText()));
                fields.put("door02Open", "0x01".equalsIgnoreCase(data.path("door02").asText()));
            }
            case "0X09" -> {
                fields.put("accessUserId", data.path("AccessUserId").asText());
                fields.put("fileName", data.path("FileName").asText());
                fields.put("mimeType", data.path("MimeType").asText());
                // 주의: Base64 이미지가 클 수 있으므로 로그나 메모리 관리에 유의해야 합니다.
                fields.put("imageBase64", data.path("ImageBase64").asText());
            }
            case "0X06" -> fields.put("updateResult", "0x00".equalsIgnoreCase(data.asText()) ? "SUCCESS" : "FAIL");
        }

        return new DeviceStatusDto(apiKey, sensorNetworkUid, DeviceType.ACCESS, cmd, Instant.now(), fields, rawPayload);
    }
}