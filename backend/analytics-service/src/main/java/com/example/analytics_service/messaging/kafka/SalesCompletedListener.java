package com.example.analytics_service.messaging.kafka;

import com.example.analytics_service.dto.SaleCompletedEvent;
import com.example.analytics_service.service.AnalyticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Messaging adapter for ventas360.sales.completed: validates and delegates to the domain service.
 * Three concurrent consumers, one per partition, inside the analytics consumer group.
 */
@Component
public class SalesCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(SalesCompletedListener.class);

    private final AnalyticsService analyticsService;
    private final EventValidator validator;

    public SalesCompletedListener(AnalyticsService analyticsService, EventValidator validator) {
        this.analyticsService = analyticsService;
        this.validator = validator;
    }

    @KafkaListener(
            id = "analytics-sales-completed",
            topics = "${ventas360.kafka.topics.sales-completed}",
            groupId = "${ventas360.kafka.consumer-group}",
            concurrency = "3",
            properties = "spring.json.value.default.type=com.example.analytics_service.dto.SaleCompletedEvent")
    public void onSaleCompleted(ConsumerRecord<String, SaleCompletedEvent> record) {
        log.info("Sale event received topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        SaleCompletedEvent event = validator.requireValid(record.value());
        boolean recorded = analyticsService.recordSale(event);
        log.info("Sale event processed saleId={} recorded={} partition={} offset={}",
                event.saleId(), recorded, record.partition(), record.offset());
    }
}
