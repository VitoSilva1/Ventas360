package com.example.analytics_service.repository;

import com.example.analytics_service.model.AnalyticsSale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsSaleRepository extends JpaRepository<AnalyticsSale, Long> {

    @Query("select coalesce(sum(s.total), 0) from AnalyticsSale s")
    BigDecimal sumTotal();

    @Query("select coalesce(sum(s.total), 0) from AnalyticsSale s where s.saleCreatedAt >= :from")
    BigDecimal sumTotalSince(LocalDateTime from);

    @Query("""
            select new com.example.analytics_service.repository.ProductSalesRow(
                l.productId, sum(l.quantity), sum(l.subtotal))
            from AnalyticsSaleLine l
            group by l.productId
            order by sum(l.quantity) desc, l.productId asc""")
    List<ProductSalesRow> topProducts(Pageable page);
}
