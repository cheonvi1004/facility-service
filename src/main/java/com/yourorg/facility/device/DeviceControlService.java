package com.yourorg.facility.device;

import com.yourorg.facility.config.FacilityCommandProperties;
import com.yourorg.facility.config.MqttProperties;
import com.yourorg.facility.correlation.PendingCommandTracker;
import com.yourorg.facility.dto.ControlRequestMessage;
import com.yourorg.facility.dto.ControlResultMessage;
import com.yourorg.facility.mqtt.MqttGateway;
import com.yourorg.facility.ws.DeviceWebSocketBroadcaster;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * web(WebSocket 또는 REST)에서 들어온 제어 요청을 MQTT로 발행하고,
 * 응답 Cmd가 지정되어 있으면 PendingCommandTracker에 등록해 타임아웃을 추적한다.
 */
@Service
public class DeviceControlService {

    private final MqttGateway mqttGateway;
    private final PendingCommandTracker pendingCommandTracker;
    private final DeviceWebSocketBroadcaster broadcaster;
    private final MqttProperties mqttProperties;
    private final FacilityCommandProperties commandProperties;

    public DeviceControlService(MqttGateway mqttGateway,
                                 PendingCommandTracker pendingCommandTracker,
                                 DeviceWebSocketBroadcaster broadcaster,
                                 MqttProperties mqttProperties,
                                 FacilityCommandProperties commandProperties) {
        this.mqttGateway = mqttGateway;
        this.pendingCommandTracker = pendingCommandTracker;
        this.broadcaster = broadcaster;
        this.mqttProperties = mqttProperties;
        this.commandProperties = commandProperties;
    }

    public void handle(ControlRequestMessage request) {
        String apiKey = mqttProperties.defaultApiKey();

        if (request.expectedResponseCmd() != null) {
            pendingCommandTracker.track(
                    request.requestId(),
                    request.sensorNetworkUid(),
                    request.cmd(),
                    request.expectedResponseCmd(),
                    Duration.ofSeconds(commandProperties.defaultTimeoutSeconds()),
                    timedOut -> broadcaster.broadcast(new ControlResultMessage(
                            timedOut.requestId(), timedOut.sensorNetworkUid(),
                            ControlResultMessage.Result.TIMEOUT, Instant.now()))
            );
        }

        mqttGateway.publishControl(apiKey, request.deviceType(), request.sensorNetworkUid(),
                request.cmd(), request.data(), null);
    }
}
