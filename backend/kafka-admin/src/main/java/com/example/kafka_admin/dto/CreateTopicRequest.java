package com.example.kafka_admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record CreateTopicRequest(
        @NotNull(message = "name is required")
        @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}", message = "name may only contain letters, digits, '.', '_' and '-'")
        String name,

        @NotNull(message = "partitions is required")
        @Min(value = 1, message = "partitions must be at least 1")
        @Max(value = 100, message = "partitions must be at most 100")
        Integer partitions,

        @NotNull(message = "replicationFactor is required")
        @Min(value = 1, message = "replicationFactor must be at least 1")
        // Upper bound keeps the value inside a short (Kafka's type); larger values would wrap to -1.
        @Max(value = 32, message = "replicationFactor must be at most 32")
        Integer replicationFactor,

        // Topic-level overrides, e.g. retention.ms, cleanup.policy, min.insync.replicas.
        Map<String, String> configs
) {
}
