package com.example.product_service.service;

import com.example.product_service.dto.ProductChangedEvent;
import com.example.product_service.dto.ProductChangedEvent.ChangeType;
import com.example.product_service.dto.SaleCompletedEvent;
import com.example.product_service.model.ProcessedSale;
import com.example.product_service.repository.ProcessedSaleRepository;
import com.example.product_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Discounts stock for completed sales. Knows nothing about Kafka: the listener hands it a validated DTO.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;
    private final ProcessedSaleRepository processedSaleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryService(ProductRepository productRepository,
                            ProcessedSaleRepository processedSaleRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.processedSaleRepository = processedSaleRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Applies a sale once. The processed-sale marker and the stock changes share one transaction,
     * so either both are stored or neither is (and the event is retried).
     *
     * @return false when the sale had already been applied
     */
    @Transactional
    public boolean applySale(SaleCompletedEvent sale) {
        if (processedSaleRepository.existsById(sale.saleId())) {
            log.warn("Sale already applied to inventory, skipping saleId={}", sale.saleId());
            return false;
        }
        processedSaleRepository.save(new ProcessedSale(sale.saleId()));

        for (SaleCompletedEvent.Item item : sale.items()) {
            if (productRepository.decrementStock(item.productId(), item.quantity()) == 0) {
                log.warn("Product not found while applying sale, item skipped saleId={} productId={}",
                        sale.saleId(), item.productId());
                continue;
            }
            productRepository.findById(item.productId()).ifPresent(product -> {
                log.info("Stock decremented saleId={} productId={} quantity={} newStock={}",
                        sale.saleId(), product.getId(), item.quantity(), product.getStock());
                // Same domain event as a manual update: relayed to Kafka after commit.
                eventPublisher.publishEvent(ProductChangedEvent.of(ChangeType.UPDATED, product));
            });
        }
        return true;
    }
}
