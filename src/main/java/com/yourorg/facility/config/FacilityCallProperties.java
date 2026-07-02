package com.yourorg.facility.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facility.call")
public record FacilityCallProperties(
        int accessTimeoutSeconds
) {
}
