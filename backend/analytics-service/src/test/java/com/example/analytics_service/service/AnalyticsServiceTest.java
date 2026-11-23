package com.example.analytics_service.service;

import com.example.analytics_service.dto.AnalyticsSummary;
import com.example.analytics_service.dto.ProductChangedEvent;
import com.example.analytics_service.dto.SaleCompletedEvent;
import com.example.analytics_service.repository.AnalyticsSaleRepository;
import com.example.analytics_service.repository.CatalogProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class AnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsSaleRepository saleRepository;

    @Autowired
    private CatalogProductRepository catalogRepository;

    @BeforeEach
    void cleanState() {
        saleRepository.deleteAll();
        catalogRepository.deleteAll();
    }

    @Test
    void recordSale_sameSaleTwice_isCountedOnce() {
        SaleCompletedEvent sale = sale(1L, line(10L, 2, "20.00"));

        assertThat(analyticsService.recordSale(sale)).isTrue();
        assertThat(analyticsService.recordSale(sale)).isFalse();

        assertThat(analyticsService.summary().totalSales()).isEqualTo(1);
    }

    @Test
    void summary_aggregatesRevenueAverageAndTopProductsWithCatalogNames() {
        analyticsService.applyProductChange(product(ProductChangedEvent.ChangeType.CREATED, 10L, "Mouse", 50));
        analyticsService.applyProductChange(product(ProductChangedEvent.ChangeType.CREATED, 20L, "Teclado", 50));
        analyticsService.recordSale(sale(1L, line(10L, 3, "30.00"), line(20L, 1, "70.00")));
        analyticsService.recordSale(sale(2L, line(10L, 2, "20.00")));

        AnalyticsSummary summary = analyticsService.summary();

        assertThat(summary.totalSales()).isEqualTo(2);
        assertThat(summary.totalRevenue()).isEqualByComparingTo("120.00");
        assertThat(summary.averageTicket()).isEqualByComparingTo("60.00");
        assertThat(summary.revenueToday()).isEqualByComparingTo("120.00");
        assertThat(summary.topProducts()).first().satisfies(top -> {
            assertThat(top.productId()).isEqualTo(10L);
            assertThat(top.name()).isEqualTo("Mouse");
            assertThat(top.unitsSold()).isEqualTo(5);
            assertThat(top.revenue()).isEqualByComparingTo("50.00");
        });
    }

    @Test
    void summary_withoutSales_returnsZeros() {
        AnalyticsSummary summary = analyticsService.summary();

        assertThat(summary.totalSales()).isZero();
        assertThat(summary.totalRevenue()).isEqualByComparingTo("0");
        assertThat(summary.averageTicket()).isEqualByComparingTo("0");
        assertThat(summary.topProducts()).isEmpty();
    }

    @Test
    void lowStock_listsActiveProductsAtOrBelowThresholdAndSkipsDeletedOnes() {
        analyticsService.applyProductChange(product(ProductChangedEvent.ChangeType.CREATED, 1L, "Cable", 2));
        analyticsService.applyProductChange(product(ProductChangedEvent.ChangeType.CREATED, 2L, "Monitor", 40));
        analyticsService.applyProductChange(product(ProductChangedEvent.ChangeType.CREATED, 3L, "Hub", 1));
        analyticsService.applyProductChange(product(ProductChangedEvent.ChangeType.DELETED, 3L, "Hub", 1));

        assertThat(analyticsService.summary().lowStock())
                .extracting(AnalyticsSummary.LowStockProduct::productId)
                .containsExactly(1L);
    }

    @Test
    void applyProductChange_updateOverwritesCatalogEntry() {
        analyticsService.applyProductChange(product(ProductChangedEvent.ChangeType.CREATED, 1L, "Cable", 20));
        analyticsService.applyProductChange(product(ProductChangedEvent.ChangeType.UPDATED, 1L, "Cable USB-C", 3));

        assertThat(analyticsService.summary().lowStock())
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.name()).isEqualTo("Cable USB-C");
                    assertThat(p.stock()).isEqualTo(3);
                });
    }

    static SaleCompletedEvent sale(Long saleId, SaleCompletedEvent.SaleCompletedItem... items) {
        BigDecimal total = List.of(items).stream()
                .map(SaleCompletedEvent.SaleCompletedItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SaleCompletedEvent(UUID.randomUUID(), saleId, "evt-" + saleId, 1L, total, "CREATED",
                LocalDateTime.now(), Instant.now(), List.of(items));
    }

    static SaleCompletedEvent.SaleCompletedItem line(Long productId, int quantity, String subtotal) {
        BigDecimal sub = new BigDecimal(subtotal);
        return new SaleCompletedEvent.SaleCompletedItem(productId, quantity,
                sub.divide(BigDecimal.valueOf(quantity)), sub);
    }

    static ProductChangedEvent product(ProductChangedEvent.ChangeType type, Long id, String name, int stock) {
        return new ProductChangedEvent(UUID.randomUUID(), type, id, name, null, new BigDecimal("1000"),
                stock, true, Instant.now());
    }
}
