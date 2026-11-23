package com.example.product_service.repository;

import com.example.product_service.model.ProcessedSale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedSaleRepository extends JpaRepository<ProcessedSale, Long> {
}
