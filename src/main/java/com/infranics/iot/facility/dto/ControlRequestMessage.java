package com.infranics.iot.facility.dto;

import com.infranics.iot.facility.device.DeviceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ControlRequestMessage(
		@NotBlank String requestId,
        @NotBlank String apiKey,
        @NotNull DeviceType deviceType,
        @NotNull String sensorName,
        @NotBlank String sensorNetworkUid,
        @NotBlank String cmd,
        Object data,
        String expectedResponseCmd
) {
}
