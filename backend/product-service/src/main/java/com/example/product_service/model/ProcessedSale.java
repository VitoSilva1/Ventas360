package com.example.product_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

/**
 * Marks a sale whose stock was already discounted, so a re-delivered Kafka event is ignored.
 * <p>
 * Implements {@link Persistable} with {@code isNew() == true}: the id is assigned (the sale id), and
 * without this Spring Data's {@code save()} would merge (SELECT, then INSERT or UPDATE). A concurrent
 * duplicate that committed in between would then become a silent UPDATE instead of a primary-key
 * violation, and the same sale would decrement stock twice.
 */
@Entity
@Table(name = "inventory_processed_sales")
public class ProcessedSale implements Persistable<Long> {

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

    @Override
    public Long getId() {
        return saleId;
    }

    /**
     * Rows are only ever inserted, never updated: always persist so a duplicate fails on the primary key.
     */
    @Override
    public boolean isNew() {
        return true;
    }
}
