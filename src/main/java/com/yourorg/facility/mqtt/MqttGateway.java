package com.yourorg.facility.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.facility.config.MqttProperties;
import com.yourorg.facility.device.DeviceType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class MqttGateway {

    private static final Logger log = LoggerFactory.getLogger(MqttGateway.class);

    private final MqttProperties properties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private MqttClient client;

    public MqttGateway(MqttProperties properties, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void connect() throws MqttException {
        client = new MqttClient(properties.brokerUrl(), properties.clientId(), new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(properties.username());
        options.setPassword(properties.password().toCharArray());
        options.setConnectionTimeout(properties.connectionTimeoutSeconds());
        options.setKeepAliveInterval(properties.keepAliveIntervalSeconds());
        options.setAutomaticReconnect(properties.automaticReconnect());
        options.setCleanSession(true);

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                log.info("MQTT connected (reconnect={}) to {}", reconnect, serverURI);
                subscribeStatusTopic();
            }

            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT connection lost: {}", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handleIncoming(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 필요 시 QoS>0 발행 확인 로직 추가
            }
        });

        client.connect(options);
    }

    private void subscribeStatusTopic() {
        try {
            client.subscribe(FacilityTopic.statusSubscribeFilter(), 1);
            log.info("Subscribed to {}", FacilityTopic.statusSubscribeFilter());
        } catch (MqttException e) {
            log.error("Failed to subscribe status topic", e);
        }
    }

    private void handleIncoming(String topic, MqttMessage message) {
        FacilityTopic.parseStatus(topic).ifPresentOrElse(parts -> {
            try {
                JsonNode json = objectMapper.readTree(message.getPayload());
                eventPublisher.publishEvent(
                        new MqttMessageReceivedEvent(this, parts.apiKey(), parts.deviceType(), json));
            } catch (Exception e) {
                log.error("Failed to parse MQTT payload on topic {}: {}", topic, e.getMessage());
            }
        }, () -> log.debug("Ignoring non-status topic: {}", topic));
    }

    /**
     * web -> svc -> 장비 제어 명령 발행.
     * device / [ApiKey] / [장비종류] / [SensorNetworkUID] / Ctrl
     */
    public void publishControl(String apiKey, DeviceType type, String sensorNetworkUid,
                                String cmd, Object data, String sensorName) {
        String topic = FacilityTopic.ctrlPublishTopic(apiKey, type, sensorNetworkUid);

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("APIKEY", apiKey);
        payload.put("SensorNetworkUID", sensorNetworkUid);
        if (sensorName != null) {
            payload.put("SensorName", sensorName);
        }
        payload.put("Cmd", cmd);
        if (data != null) {
            payload.put("Data", data);
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            MqttMessage mqttMessage = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            mqttMessage.setQos(1);
            client.publish(topic, mqttMessage);
            log.debug("Published control cmd={} to topic={}", cmd, topic);
        } catch (Exception e) {
            log.error("Failed to publish control command to {}: {}", topic, e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            log.warn("Error disconnecting MQTT client: {}", e.getMessage());
        }
    }
}
