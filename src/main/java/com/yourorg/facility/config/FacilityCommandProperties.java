package com.yourorg.facility.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facility.command")
public record FacilityCommandProperties(
        int defaultTimeoutSeconds
) {
}
