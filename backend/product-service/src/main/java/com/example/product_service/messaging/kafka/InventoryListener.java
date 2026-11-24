package com.example.product_service.messaging.kafka;

import com.example.product_service.dto.SaleCompletedEvent;
import com.example.product_service.service.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Messaging adapter for ventas360.sales.completed in the inventory consumer group.
 * analytics-service reads the same topic in its own group: each group gets every event.
 */
@Component
public class InventoryListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryListener.class);

    private final InventoryService inventoryService;
    private final EventValidator validator;

    public InventoryListener(InventoryService inventoryService, EventValidator validator) {
        this.inventoryService = inventoryService;
        this.validator = validator;
    }

    @KafkaListener(
            id = "inventory-sales-completed",
            topics = "${ventas360.kafka.topics.sales-completed}",
            groupId = "${ventas360.kafka.consumer-group}",
            concurrency = "3",
            properties = "spring.json.value.default.type=com.example.product_service.dto.SaleCompletedEvent")
    public void onSaleCompleted(ConsumerRecord<String, SaleCompletedEvent> record) {
        log.info("Sale event received topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        SaleCompletedEvent sale = validator.requireValid(record.value());
        boolean applied = inventoryService.applySale(sale);
        log.info("Sale event processed saleId={} applied={} partition={} offset={}",
                sale.saleId(), applied, record.partition(), record.offset());
    }
}
