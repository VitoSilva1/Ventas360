package com.example.sales_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published to the {@code ventas360.sales.completed} Kafka topic once a sale is persisted.
 * Keyed by {@code saleId}, so every event for the same sale lands in the same partition.
 */
public record SaleCompletedEvent(
        UUID eventId,
        Long saleId,
        String saleEventId,
        Long customerId,
        BigDecimal total,
        String status,
        LocalDateTime createdAt,
        Instant publishedAt,
        List<SaleCompletedItem> items
) {
    public static SaleCompletedEvent from(SaleResponse sale, String saleEventId) {
        return new SaleCompletedEvent(
                UUID.randomUUID(),
                sale.id(),
                saleEventId,
                sale.customerId(),
                sale.total(),
                sale.status() == null ? null : sale.status().name(),
                sale.createdAt(),
                Instant.now(),
                sale.items().stream().map(SaleCompletedItem::from).toList()
        );
    }

    public record SaleCompletedItem(
            Long productId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
        static SaleCompletedItem from(SaleItemResponse item) {
            return new SaleCompletedItem(item.productId(), item.quantity(), item.unitPrice(), item.subtotal());
        }
    }
}
