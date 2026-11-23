package com.example.analytics_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consumer-side contract of the {@code ventas360.sales.completed} topic (published by sales-service).
 * The constraints describe the minimum a sale needs to be useful for analytics.
 */
public record SaleCompletedEvent(
        UUID eventId,
        @NotNull Long saleId,
        String saleEventId,
        Long customerId,
        @NotNull @PositiveOrZero BigDecimal total,
        String status,
        LocalDateTime createdAt,
        Instant publishedAt,
        @NotEmpty List<@Valid SaleCompletedItem> items
) {
    public record SaleCompletedItem(
            @NotNull Long productId,
            @NotNull @Positive Integer quantity,
            BigDecimal unitPrice,
            @NotNull @PositiveOrZero BigDecimal subtotal
    ) {
    }
}
