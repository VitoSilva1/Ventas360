package com.example.product_service.messaging.kafka;

import com.example.product_service.dto.ProductChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Relays committed product changes to Kafka. Running AFTER_COMMIT guarantees that a rolled-back
 * change never produces an event; a Kafka failure is logged and never undoes the committed change.
 */
@Component
public class ProductEventsKafkaRelay {

    private static final Logger log = LoggerFactory.getLogger(ProductEventsKafkaRelay.class);

    private final KafkaTemplate<String, ProductChangedEvent> kafkaTemplate;
    private final String topic;

    public ProductEventsKafkaRelay(
            KafkaTemplate<String, ProductChangedEvent> kafkaTemplate,
            @Value("${ventas360.kafka.topics.product-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(ProductChangedEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.productId()), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Product event NOT published topic={} productId={} type={} eventId={}",
                                topic, event.productId(), event.type(), event.eventId(), error);
                        return;
                    }
                    var metadata = result.getRecordMetadata();
                    log.info("Product event published topic={} partition={} offset={} productId={} type={} eventId={}",
                            metadata.topic(), metadata.partition(), metadata.offset(),
                            event.productId(), event.type(), event.eventId());
                });
    }
}
