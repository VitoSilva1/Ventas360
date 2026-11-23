package com.example.analytics_service.service;

import com.example.analytics_service.dto.AnalyticsSummary;
import com.example.analytics_service.dto.ProductChangedEvent;
import com.example.analytics_service.dto.SaleCompletedEvent;
import com.example.analytics_service.model.AnalyticsSale;
import com.example.analytics_service.model.CatalogProduct;
import com.example.analytics_service.repository.AnalyticsSaleRepository;
import com.example.analytics_service.repository.CatalogProductRepository;
import com.example.analytics_service.repository.ProductSalesRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Analytics domain logic. It knows nothing about Kafka: listeners hand it already validated DTOs.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int TOP_PRODUCTS = 5;

    private final AnalyticsSaleRepository saleRepository;
    private final CatalogProductRepository catalogRepository;
    private final int lowStockThreshold;

    public AnalyticsService(AnalyticsSaleRepository saleRepository,
                            CatalogProductRepository catalogRepository,
                            @Value("${ventas360.analytics.low-stock-threshold:5}") int lowStockThreshold) {
        this.saleRepository = saleRepository;
        this.catalogRepository = catalogRepository;
        this.lowStockThreshold = lowStockThreshold;
    }

    /**
     * Records a completed sale once. Returns false when the sale was already recorded
     * (Kafka delivers at least once, so duplicates are expected and ignored).
     */
    @Transactional
    public boolean recordSale(SaleCompletedEvent event) {
        if (saleRepository.existsById(event.saleId())) {
            log.warn("Duplicate sale event ignored saleId={} eventId={}", event.saleId(), event.eventId());
            return false;
        }
        AnalyticsSale sale = new AnalyticsSale(event.saleId(), event.customerId(), event.total(), event.createdAt());
        event.items().forEach(item -> sale.addLine(item.productId(), item.quantity(), item.subtotal()));
        saleRepository.save(sale);
        return true;
    }

    /**
     * Upserts the local catalog entry. Replaying the compacted topic converges to the latest state.
     */
    @Transactional
    public void applyProductChange(ProductChangedEvent event) {
        CatalogProduct product = catalogRepository.findById(event.productId())
                .orElseGet(() -> new CatalogProduct(event.productId()));
        boolean deleted = event.type() == ProductChangedEvent.ChangeType.DELETED;
        product.apply(event.name(), event.price(), event.stock(), event.active(), deleted);
        catalogRepository.save(product);
    }

    @Transactional(readOnly = true)
    public boolean isSaleRecorded(Long saleId) {
        return saleRepository.existsById(saleId);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummary summary() {
        long totalSales = saleRepository.count();
        BigDecimal totalRevenue = saleRepository.sumTotal();
        BigDecimal averageTicket = totalSales == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP);
        BigDecimal revenueToday = saleRepository.sumTotalSince(LocalDate.now().atStartOfDay());

        List<ProductSalesRow> rows = saleRepository.topProducts(PageRequest.of(0, TOP_PRODUCTS));
        Map<Long, String> names = catalogRepository.findAllById(rows.stream().map(ProductSalesRow::productId).toList())
                .stream()
                .collect(Collectors.toMap(CatalogProduct::getProductId,
                        p -> p.getName() == null ? "" : p.getName(), (a, b) -> a));
        List<AnalyticsSummary.TopProduct> topProducts = rows.stream()
                .map(r -> new AnalyticsSummary.TopProduct(r.productId(),
                        names.getOrDefault(r.productId(), "Producto " + r.productId()),
                        r.unitsSold(), r.revenue()))
                .toList();

        List<AnalyticsSummary.LowStockProduct> lowStock = catalogRepository
                .findByActiveTrueAndDeletedFalseAndStockLessThanEqualOrderByStockAsc(lowStockThreshold)
                .stream()
                .map(p -> new AnalyticsSummary.LowStockProduct(p.getProductId(), p.getName(), p.getStock()))
                .toList();

        return new AnalyticsSummary(totalSales, totalRevenue, averageTicket, revenueToday,
                topProducts, lowStock, Instant.now());
    }
}
