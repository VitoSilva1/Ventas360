package com.example.rabbit_mq.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SaleEventItemDto(
    @NotNull Long productId,
    String productName,
    @NotNull @Positive Integer quantity,
    @NotNull @DecimalMin(value = "0.0") BigDecimal unitPrice
) {}
