package com.example.analytics_service.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One consumed sale. The primary key is the sales-service sale id, which makes
 * re-delivered events (at-least-once) idempotent.
 */
@Entity
@Table(name = "analytics_sales")
public class AnalyticsSale {

    @Id
    private Long saleId;

    private Long customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    private LocalDateTime saleCreatedAt;

    @Column(nullable = false)
    private Instant receivedAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalyticsSaleLine> lines = new ArrayList<>();

    protected AnalyticsSale() {
    }

    public AnalyticsSale(Long saleId, Long customerId, BigDecimal total, LocalDateTime saleCreatedAt) {
        this.saleId = saleId;
        this.customerId = customerId;
        this.total = total;
        this.saleCreatedAt = saleCreatedAt;
        this.receivedAt = Instant.now();
    }

    public void addLine(Long productId, int quantity, BigDecimal subtotal) {
        lines.add(new AnalyticsSaleLine(this, productId, quantity, subtotal));
    }

    public Long getSaleId() {
        return saleId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<AnalyticsSaleLine> getLines() {
        return lines;
    }
}
