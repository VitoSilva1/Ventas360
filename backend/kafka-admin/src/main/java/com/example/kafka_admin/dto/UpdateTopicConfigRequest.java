package com.example.kafka_admin.dto;

import jakarta.validation.constraints.AssertTrue;

import java.util.List;
import java.util.Map;

/**
 * Incremental change of topic configs: {@code set} adds or overwrites overrides,
 * {@code delete} removes overrides so the broker default applies again.
 */
public record UpdateTopicConfigRequest(
        Map<String, String> set,
        List<String> delete
) {
    @AssertTrue(message = "at least one config must be set or deleted")
    public boolean isNotEmpty() {
        return (set != null && !set.isEmpty()) || (delete != null && !delete.isEmpty());
    }
}
