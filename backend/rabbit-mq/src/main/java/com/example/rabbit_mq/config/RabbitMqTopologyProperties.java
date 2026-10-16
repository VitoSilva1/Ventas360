package com.example.rabbit_mq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq.ventas360")
public record RabbitMqTopologyProperties(Exchange exchange, Queues queues, Dlx dlx, Dlq dlq) {

    public record Exchange(String salesFanout) {}
    public record Queues(String sales, String mail) {}
    public record Dlx(String name) {}
    public record Dlq(String sales, String mail) {}
}
