package com.example.analytics_service.repository;

import com.example.analytics_service.model.CatalogProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, Long> {

    List<CatalogProduct> findByActiveTrueAndDeletedFalseAndStockLessThanEqualOrderByStockAsc(int threshold);
}
