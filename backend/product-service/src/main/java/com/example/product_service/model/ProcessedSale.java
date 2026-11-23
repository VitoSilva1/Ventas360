package com.example.product_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Marks a sale whose stock was already discounted, so a re-delivered Kafka event is ignored.
 */
@Entity
@Table(name = "inventory_processed_sales")
public class ProcessedSale {

    @Id
    private Long saleId;

    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessedSale() {
    }

    public ProcessedSale(Long saleId) {
        this.saleId = saleId;
        this.processedAt = Instant.now();
    }

    public Long getSaleId() {
        return saleId;
    }
}
