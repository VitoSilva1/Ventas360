package com.example.sales_service.messaging.kafka;

/**
 * Raised when an event could not be confirmed by the Kafka brokers within the send timeout.
 */
public class KafkaPublishException extends RuntimeException {

    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
