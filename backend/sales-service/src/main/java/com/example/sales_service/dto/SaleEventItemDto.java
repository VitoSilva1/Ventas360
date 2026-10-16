package com.example.sales_service.dto;

import java.math.BigDecimal;

public record SaleEventItemDto(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
