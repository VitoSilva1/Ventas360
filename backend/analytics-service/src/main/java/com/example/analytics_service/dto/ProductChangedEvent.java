package com.example.analytics_service.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side contract of the compacted {@code ventas360.product.events} topic (published by product-service).
 */
public record ProductChangedEvent(
        UUID eventId,
        @NotNull ChangeType type,
        @NotNull Long productId,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        boolean active,
        Instant occurredAt
) {
    public enum ChangeType {
        CREATED, UPDATED, DELETED
    }
}
