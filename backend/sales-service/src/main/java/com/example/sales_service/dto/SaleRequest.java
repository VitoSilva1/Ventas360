package com.example.sales_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaleRequest(
        Long customerId,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<@Valid SaleItemRequest> items
) {
}
