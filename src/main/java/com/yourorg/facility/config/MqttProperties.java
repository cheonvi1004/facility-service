package com.yourorg.facility.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facility.mqtt")
public record MqttProperties(
        String brokerUrl,
        String clientId,
        String username,
        String password,
        String defaultApiKey,
        int connectionTimeoutSeconds,
        int keepAliveIntervalSeconds,
        boolean automaticReconnect
) {
}
