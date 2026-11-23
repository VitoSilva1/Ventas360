package com.example.analytics_service.messaging.kafka;

import com.example.analytics_service.dto.ProductChangedEvent;
import com.example.analytics_service.service.AnalyticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Messaging adapter for the compacted ventas360.product.events topic. With auto-offset-reset=earliest,
 * a brand-new analytics group replays the compacted log and rebuilds the whole catalog.
 */
@Component
public class ProductEventsListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventsListener.class);

    private final AnalyticsService analyticsService;
    private final EventValidator validator;

    public ProductEventsListener(AnalyticsService analyticsService, EventValidator validator) {
        this.analyticsService = analyticsService;
        this.validator = validator;
    }

    @KafkaListener(
            id = "analytics-product-events",
            topics = "${ventas360.kafka.topics.product-events}",
            groupId = "${ventas360.kafka.consumer-group}",
            concurrency = "3",
            properties = "spring.json.value.default.type=com.example.analytics_service.dto.ProductChangedEvent")
    public void onProductChanged(ConsumerRecord<String, ProductChangedEvent> record) {
        log.info("Product event received topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        ProductChangedEvent event = validator.requireValid(record.value());
        analyticsService.applyProductChange(event);
        log.info("Product event processed productId={} type={} partition={} offset={}",
                event.productId(), event.type(), record.partition(), record.offset());
    }
}
