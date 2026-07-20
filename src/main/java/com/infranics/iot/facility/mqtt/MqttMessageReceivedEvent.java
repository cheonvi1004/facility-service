package com.infranics.iot.facility.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.infranics.iot.facility.device.DeviceType;

import org.springframework.context.ApplicationEvent;

/**
 * MqttGateway가 Status 토픽 메시지를 수신했을 때 발행하는 이벤트.
 * MqttGateway는 라우팅 로직을 모르고, 리스너(DeviceMessageDispatcher)가
 * 정규화/브로드캐스트/통화세션 처리를 담당한다.
 * (MqttGateway <-> CallSessionManager 간 순환 의존을 피하기 위한 구조)
 */
public class MqttMessageReceivedEvent extends ApplicationEvent {

    private final String apiKey;
    private final DeviceType deviceType;
    private final JsonNode payload;

    public MqttMessageReceivedEvent(Object source, String apiKey, DeviceType deviceType, JsonNode payload) {
        super(source);
        this.apiKey = apiKey;
        this.deviceType = deviceType;
        this.payload = payload;
    }

    public String apiKey() {
        return apiKey;
    }

    public DeviceType deviceType() {
        return deviceType;
    }

    public JsonNode payload() {
        return payload;
    }
}
