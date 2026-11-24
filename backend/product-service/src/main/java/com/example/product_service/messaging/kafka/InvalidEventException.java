package com.example.product_service.messaging.kafka;

/**
 * The payload deserialized but breaks the event contract. Retrying cannot fix it,
 * so the error handler sends it straight to the dead-letter topic.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
