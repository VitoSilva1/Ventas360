package com.example.analytics_service.repository;

import java.math.BigDecimal;

public record ProductSalesRow(Long productId, Long unitsSold, BigDecimal revenue) {
}
