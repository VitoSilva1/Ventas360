package com.example.kafka_admin.exception;

import org.springframework.http.HttpStatus;

/**
 * A Kafka admin operation failed; carries the HTTP status the API should answer with.
 */
public class KafkaAdminOperationException extends RuntimeException {

    private final HttpStatus status;

    public KafkaAdminOperationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public KafkaAdminOperationException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
