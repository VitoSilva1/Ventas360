package com.example.kafka_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResetOffsetsRequest(
        @NotBlank(message = "topic is required")
        String topic,

        @NotNull(message = "strategy is required (EARLIEST or LATEST)")
        Strategy strategy
) {
    public enum Strategy {
        EARLIEST, LATEST
    }
}
