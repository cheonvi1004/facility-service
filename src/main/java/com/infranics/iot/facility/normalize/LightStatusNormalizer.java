package com.infranics.iot.facility.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class LightStatusNormalizer implements DeviceStatusNormalizer {

    @Override
    public DeviceType supports() {
        return DeviceType.LIGHT;
    }

    @Override
    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
        JsonNode data = rawPayload.path("Data");
        Map<String, Object> fields = new HashMap<>();

        if ("0x2A".equalsIgnoreCase(cmd)) {
            fields.put("ledTimer", data.path("Ledtimer").asInt(0));
            fields.put("blockCount", data.path("BlockCount").asInt(0));
            
            // Hex 문자열 배열을 비트 연산하여 개별 LED 상태(Boolean) 리스트로 변환
            fields.put("lightPowerStates", parseHexArrayToBooleans(data.path("LightPwrState")));
            fields.put("pirStates", parseHexArrayToBooleans(data.path("PIRState")));
            fields.put("lightFailStates", parseHexArrayToBooleans(data.path("LightFailState")));
        } else {
            // 0x3A 등 제어 응답인 경우
            fields.put("resultData", data.asText());
        }

        return new DeviceStatusDto(apiKey, sensorNetworkUid, DeviceType.LIGHT, cmd, Instant.now(), fields, rawPayload);
    }

    /**
     * "0xFE"와 같은 Hex 문자열 배열을 받아, Bit 연산을 통해 
     * 개별 Boolean(true/false) 상태 리스트로 풀어서 반환합니다.
     */
    private List<Boolean> parseHexArrayToBooleans2(JsonNode arrayNode) {
        List<Boolean> bitStates = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                // 1. "0x" 접두사 제거
                String hexStr = node.asText().replaceAll("^0[xX]", ""); 
                if (hexStr.isEmpty()) continue;

                try {
                    // 2. 16진수 문자열을 숫자로 변환
                    int val = Integer.parseInt(hexStr, 16);
                    
                    // 3. 비트 마스킹 (1111 1110 = 0xFE의 경우 LSB(첫 번째 비트)가 0)
                    for (int i = 0; i < 8; i++) {
                        // 오른쪽에서 i번째 비트가 1인지 확인합니다.
                        boolean isBitOn = (val & (1 << i)) != 0; 
                        bitStates.add(isBitOn);
                    }
                } catch (NumberFormatException e) {
                    // 변환 실패 시 안전하게 기본값(false) 8개 채움
                    for (int i = 0; i < 8; i++) {
                        bitStates.add(false);
                    }
                }
            }
        }
        return bitStates;
    }
    
    /**
     * "0xFE"와 같은 Hex 문자열 배열을 받아, Bit 연산을 통해 
     * 개별 Boolean(true/false) 상태 리스트로 풀어서 반환합니다.
     * (MSB 우선: 가장 왼쪽 비트가 첫 번째 LED 상태)
     */
    private List<Boolean> parseHexArrayToBooleans(JsonNode arrayNode) {
        List<Boolean> bitStates = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                String hexStr = node.asText().replaceAll("^0[xX]", ""); 
                if (hexStr.isEmpty()) continue;

                try {
                    int val = Integer.parseInt(hexStr, 16);
                    
                    // 장비 스펙 반영: 7번째 비트(가장 왼쪽)부터 0번째 비트(가장 오른쪽)순으로 검사
                    for (int i = 0; i < 8; i++) {
                        // i = 0일 때 (1 << 7) 즉 1000 0000 비트와 마스킹
                        boolean isBitOn = (val & (1 << (7 - i))) != 0; 
                        bitStates.add(isBitOn);
                    }
                } catch (NumberFormatException e) {
                    for (int i = 0; i < 8; i++) {
                        bitStates.add(false);
                    }
                }
            }
        }
        
        List<Boolean> reversed = IntStream.range(0, bitStates.size())
                .mapToObj(i -> bitStates.get(bitStates.size() - 1 - i))
                .collect(Collectors.toList());
        return reversed;
    }
}