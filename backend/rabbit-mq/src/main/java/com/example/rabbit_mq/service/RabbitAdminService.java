package com.example.rabbit_mq.service;

import com.example.rabbit_mq.exception.RabbitResourceNotFoundException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    public void createQueue(String name, boolean durable) {
        rabbitAdmin.declareQueue(new Queue(name, durable));
    }

    public void deleteQueue(String name) {
        if (rabbitAdmin.getQueueProperties(name) == null) {
            throw new RabbitResourceNotFoundException("Queue '" + name + "' not found");
        }
        rabbitAdmin.deleteQueue(name);
    }

    public void createExchange(String name, String type, boolean durable) {
        AbstractExchange exchange = switch (type) {
            case "fanout" -> new FanoutExchange(name, durable, false);
            case "direct" -> new DirectExchange(name, durable, false);
            case "topic" -> new TopicExchange(name, durable, false);
            case "headers" -> new HeadersExchange(name, durable, false);
            default -> throw new IllegalArgumentException("Unknown exchange type: " + type);
        };
        rabbitAdmin.declareExchange(exchange);
    }

    public void deleteExchange(String name) {
        rabbitAdmin.deleteExchange(name);
    }
}
