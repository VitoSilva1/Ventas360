package com.example.product_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Consumer-side view of the {@code ventas360.sales.completed} topic: inventory only needs
 * which products were sold and how many units. Other fields of the event are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SaleCompletedEvent(
        @NotNull Long saleId,
        @NotEmpty List<@Valid Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @NotNull Long productId,
            @NotNull @Positive Integer quantity
    ) {
    }
}
