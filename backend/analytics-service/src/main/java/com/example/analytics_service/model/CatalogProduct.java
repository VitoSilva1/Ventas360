package com.example.analytics_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Local read model of the product catalog, rebuilt from the compacted product-events topic.
 */
@Entity
@Table(name = "analytics_catalog_products")
public class CatalogProduct {

    @Id
    private Long productId;

    @Column(length = 120)
    private String name;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    private Integer stock;

    private boolean active;

    private boolean deleted;

    private Instant updatedAt;

    protected CatalogProduct() {
    }

    public CatalogProduct(Long productId) {
        this.productId = productId;
    }

    public void apply(String name, BigDecimal price, Integer stock, boolean active, boolean deleted) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.active = active;
        this.deleted = deleted;
        this.updatedAt = Instant.now();
    }

    public Long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public Integer getStock() {
        return stock;
    }
}
