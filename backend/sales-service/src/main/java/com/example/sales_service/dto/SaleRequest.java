package com.example.sales_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record SaleRequest(
        @NotNull(message = "El cliente es obligatorio")
        @Positive(message = "El id del cliente debe ser positivo")
        Long customerId,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<@Valid SaleItemRequest> items
) {
}
