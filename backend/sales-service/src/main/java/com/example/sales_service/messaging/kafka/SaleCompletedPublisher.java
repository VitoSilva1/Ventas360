package com.example.sales_service.messaging.kafka;

import com.example.sales_service.dto.SaleCompletedEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes {@link SaleCompletedEvent} records to the sales-completed topic, keyed by sale id.
 */
@Component
public class SaleCompletedPublisher {

    private static final Logger log = LoggerFactory.getLogger(SaleCompletedPublisher.class);

    private final KafkaTemplate<String, SaleCompletedEvent> kafkaTemplate;
    private final String topic;
    private final Duration sendTimeout;

    public SaleCompletedPublisher(
            KafkaTemplate<String, SaleCompletedEvent> kafkaTemplate,
            @Value("${ventas360.kafka.topics.sales-completed}") String topic,
            @Value("${ventas360.kafka.send-timeout:10s}") Duration sendTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.sendTimeout = sendTimeout;
    }

    /**
     * Sends the event and blocks until the brokers acknowledge it (acks=all).
     * Used when the caller must fail, and be retried, if Kafka is unavailable.
     */
    public void publishAndWait(SaleCompletedEvent event) {
        try {
            SendResult<String, SaleCompletedEvent> result =
                    send(event).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            logPublished(event, result.getRecordMetadata());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure(event, e);
        } catch (ExecutionException | TimeoutException e) {
            throw failure(event, e);
        }
    }

    /**
     * Sends the event without blocking; failures are logged, not propagated.
     */
    public void publishAsync(SaleCompletedEvent event) {
        send(event).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Sale completed event NOT published topic={} saleId={} eventId={}",
                        topic, event.saleId(), event.eventId(), error);
            } else {
                logPublished(event, result.getRecordMetadata());
            }
        });
    }

    private CompletableFuture<SendResult<String, SaleCompletedEvent>> send(SaleCompletedEvent event) {
        return kafkaTemplate.send(topic, String.valueOf(event.saleId()), event);
    }

    private void logPublished(SaleCompletedEvent event, RecordMetadata metadata) {
        log.info("Sale completed event published topic={} partition={} offset={} saleId={} eventId={}",
                metadata.topic(), metadata.partition(), metadata.offset(), event.saleId(), event.eventId());
    }

    private KafkaPublishException failure(SaleCompletedEvent event, Exception cause) {
        log.error("Sale completed event NOT published topic={} saleId={} eventId={}",
                topic, event.saleId(), event.eventId(), cause);
        return new KafkaPublishException("Could not publish sale " + event.saleId() + " to " + topic, cause);
    }
}
