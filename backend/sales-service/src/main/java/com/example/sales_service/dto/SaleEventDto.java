package com.example.sales_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleEventDto(
        UUID saleId,
        Long customerId,
        String customerEmail,
        LocalDateTime saleDate,
        List<SaleEventItemDto> items
) {
}
