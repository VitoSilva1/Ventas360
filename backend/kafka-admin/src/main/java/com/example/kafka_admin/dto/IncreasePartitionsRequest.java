package com.example.kafka_admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record IncreasePartitionsRequest(
        @NotNull(message = "totalCount is required")
        @Min(value = 1, message = "totalCount must be at least 1")
        @Max(value = 100, message = "totalCount must be at most 100")
        Integer totalCount
) {
}
