package com.yourorg.facility.dto;

import com.yourorg.facility.device.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ControlRequestMessage(
        @NotBlank String requestId,
        @NotNull DeviceType deviceType,
        @NotBlank String sensorNetworkUid,
        @NotBlank String cmd,
        Object data,
        String expectedResponseCmd
) {
}
