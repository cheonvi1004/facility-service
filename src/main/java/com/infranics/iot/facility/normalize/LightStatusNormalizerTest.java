package com.infranics.iot.facility.normalize;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.infranics.iot.facility.device.DeviceType;
//import org.springframework.stereotype.Component;
//
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class LightStatusNormalizerTest {
//
//	
//
//    public DeviceStatusDto normalize(String apiKey, String sensorNetworkUid, String cmd, JsonNode rawPayload) {
//        JsonNode data = rawPayload.path("Data");
//        Map<String, Object> fields = new HashMap<>();
//
//        if ("0x2A".equalsIgnoreCase(cmd)) {
//            fields.put("ledTimer", data.path("Ledtimer").asInt(0));
//            fields.put("blockCount", data.path("BlockCount").asInt(0));
//            
//            // Hex 문자열 배열을 비트 연산하여 개별 LED 상태(Boolean) 리스트로 변환
//            fields.put("lightPowerStates", parseHexArrayToBooleans(data.path("LightPwrState")));
//            fields.put("pirStates", parseHexArrayToBooleans(data.path("PIRState")));
//            fields.put("lightFailStates", parseHexArrayToBooleans(data.path("LightFailState")));
//        } else {
//            // 0x3A 등 제어 응답인 경우
//            fields.put("resultData", data.asText());
//        }
//
//        return new DeviceStatusDto(apiKey, sensorNetworkUid, DeviceType.LIGHT, cmd, Instant.now(), fields, rawPayload);
//    }
//
//    /**
//     * "0xFE"와 같은 Hex 문자열 배열을 받아, Bit 연산을 통해 
//     * 개별 Boolean(true/false) 상태 리스트로 풀어서 반환합니다.
//     */
//    private List<Boolean> parseHexArrayToBooleans2(JsonNode arrayNode) {
//        List<Boolean> bitStates = new ArrayList<>();
//        if (arrayNode.isArray()) {
//            for (JsonNode node : arrayNode) {
//                // 1. "0x" 접두사 제거
//                String hexStr = node.asText().replaceAll("^0[xX]", ""); 
//                if (hexStr.isEmpty()) continue;
//
//                try {
//                    // 2. 16진수 문자열을 숫자로 변환
//                    int val = Integer.parseInt(hexStr, 16);
//                    
//                    // 3. 비트 마스킹 (1111 1110 = 0xFE의 경우 LSB(첫 번째 비트)가 0)
//                    for (int i = 0; i < 8; i++) {
//                        // 오른쪽에서 i번째 비트가 1인지 확인합니다.
//                        boolean isBitOn = (val & (1 << i)) != 0; 
//                        bitStates.add(isBitOn);
//                    }
//                } catch (NumberFormatException e) {
//                    // 변환 실패 시 안전하게 기본값(false) 8개 채움
//                    for (int i = 0; i < 8; i++) {
//                        bitStates.add(false);
//                    }
//                }
//            }
//        }
//        return bitStates;
//    }
//    
//    /**
//     * "0xFE"와 같은 Hex 문자열 배열을 받아, Bit 연산을 통해 
//     * 개별 Boolean(true/false) 상태 리스트로 풀어서 반환합니다.
//     * (MSB 우선: 가장 왼쪽 비트가 첫 번째 LED 상태)
//     */
//    private List<Boolean> parseHexArrayToBooleans(JsonNode arrayNode) {
//        List<Boolean> bitStates = new ArrayList<>();
//        if (arrayNode.isArray()) {
//            for (JsonNode node : arrayNode) {
//                String hexStr = node.asText().replaceAll("^0[xX]", ""); 
//                if (hexStr.isEmpty()) continue;
//
//                try {
//                    int val = Integer.parseInt(hexStr, 16);
//                    
//                    // 장비 스펙 반영: 7번째 비트(가장 왼쪽)부터 0번째 비트(가장 오른쪽)순으로 검사
//                    for (int i = 0; i < 8; i++) {
//                        // i = 0일 때 (1 << 7) 즉 1000 0000 비트와 마스킹
//                        boolean isBitOn = (val & (1 << (7 - i))) != 0; 
//                        bitStates.add(isBitOn);
//                    }
//                } catch (NumberFormatException e) {
//                    for (int i = 0; i < 8; i++) {
//                        bitStates.add(false);
//                    }
//                }
//            }
//        }
//        return bitStates;
//    }
//    
//    public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
//		
//		LightStatusNormalizerTest test = new LightStatusNormalizerTest();
//		
//		String apiKey="004-26-6-0801";
//		String sensorNetworkUid="0001-01-0-10-1" ;
//		String cmd="0x2A";
//		
//		String jsonString = "{\"APIKEY\":\"0004-2606-0801\",\"SensorNetworkUID\":\"0001-01-01-01\",\"SensorName\":\"조명제어\",\"Cmd\":\"0x2A\",\"Data\":{\"LightPwrState\":[\"0xFF\",\"0xFF\",\"0xFF\",\"0xFF\",\"0xFF\",\"0xFF\",\"0x73\"],\"PIRState\":[\"0x00\",\"0x00\",\"0x00\",\"0x00\",\"0x00\",\"0x00\",\"0x00\"],\"LightFailState\":[\"0x00\",\"0x00\",\"0x00\",\"0x00\",\"0x00\",\"0x00\",\"0x00\"],\"Ledtimer\":10,\"BlockCount\":14}}";
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        
//        // 1. JSON 문자열을 JsonNode로 파싱
//        JsonNode rawPayload = objectMapper.readTree(jsonString);
//		
//        DeviceStatusDto t = test.normalize(apiKey, sensorNetworkUid, cmd, rawPayload);
//        
//        if(t!=null) {
//        	// 1. Java 8 날짜/시간 모듈 등록 (핵심 해결책)
//        	objectMapper.registerModule(new JavaTimeModule());
//
//        	// 2. (선택 사항) 날짜를 타임스탬프 숫자 배열 대신 ISO-8601 문자열("2026-07-02T07:36:00Z")로 예쁘게 출력하고 싶다면 추가
//        	objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        	System.out.println(objectMapper.writeValueAsString(t));
//        }
//		
//	}
//}

