package com.yourorg.facility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CallResponseMessage(
        @NotBlank String callId,
        @NotNull Action action
) {
    public enum Action { ACCEPT, REJECT }
}
