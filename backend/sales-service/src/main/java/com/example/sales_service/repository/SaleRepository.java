package com.example.sales_service.repository;

import com.example.sales_service.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsBySaleEventId(String saleEventId);

    Optional<Sale> findBySaleEventId(String saleEventId);
}
