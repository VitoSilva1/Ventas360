package com.example.product_service.dto;

import com.example.product_service.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event raised by {@code ProductService} and relayed to the compacted
 * {@code ventas360.product.events} Kafka topic after the transaction commits.
 * Keyed by {@code productId}: compaction keeps the latest state of every product.
 */
public record ProductChangedEvent(
        UUID eventId,
        ChangeType type,
        Long productId,
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

    public static ProductChangedEvent of(ChangeType type, Product product) {
        return new ProductChangedEvent(
                UUID.randomUUID(),
                type,
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.isActive(),
                Instant.now()
        );
    }
}
