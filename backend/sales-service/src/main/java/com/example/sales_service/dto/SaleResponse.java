package com.example.sales_service.dto;

import com.example.sales_service.model.Sale;
import com.example.sales_service.model.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        Long customerId,
        BigDecimal total,
        SaleStatus status,
        LocalDateTime createdAt,
        List<SaleItemResponse> items
) {
    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getCustomerId(),
                sale.getTotal(),
                sale.getStatus(),
                sale.getCreatedAt(),
                sale.getItems().stream().map(SaleItemResponse::from).toList()
        );
    }
}
