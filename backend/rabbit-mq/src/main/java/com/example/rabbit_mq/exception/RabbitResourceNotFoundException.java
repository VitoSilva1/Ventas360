package com.example.rabbit_mq.exception;

public class RabbitResourceNotFoundException extends RuntimeException {
    public RabbitResourceNotFoundException(String message) {
        super(message);
    }
}
