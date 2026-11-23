package com.example.analytics_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AnalyticsSummary(
        long totalSales,
        BigDecimal totalRevenue,
        BigDecimal averageTicket,
        BigDecimal revenueToday,
        List<TopProduct> topProducts,
        List<LowStockProduct> lowStock,
        Instant generatedAt
) {
    public record TopProduct(Long productId, String name, long unitsSold, BigDecimal revenue) {
    }

    public record LowStockProduct(Long productId, String name, int stock) {
    }
}
