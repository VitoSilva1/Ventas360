package com.example.rabbit_mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqTopologyProperties.class)
public class RabbitMqTopologyConfig {

    private final RabbitMqTopologyProperties properties;

    public RabbitMqTopologyConfig(RabbitMqTopologyProperties properties) {
        this.properties = properties;
    }

    // --- Main exchange ---

    @Bean
    public FanoutExchange salesFanoutExchange() {
        return new FanoutExchange(properties.exchange().salesFanout());
    }

    // --- Dead-letter exchange ---

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(properties.dlx().name());
    }

    // --- Dead-letter queues ---

    @Bean
    public Queue salesDlq() {
        return QueueBuilder.durable(properties.dlq().sales()).build();
    }

    @Bean
    public Queue mailDlq() {
        return QueueBuilder.durable(properties.dlq().mail()).build();
    }

    // --- DLQ bindings ---

    @Bean
    public Binding salesDlqBinding(Queue salesDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(salesDlq).to(dlxExchange).with(properties.dlq().sales());
    }

    @Bean
    public Binding mailDlqBinding(Queue mailDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(mailDlq).to(dlxExchange).with(properties.dlq().mail());
    }

    // --- Main queues (with x-dead-letter-exchange args) ---

    @Bean
    public Queue salesQueue() {
        return QueueBuilder.durable(properties.queues().sales())
                .withArgument("x-dead-letter-exchange", properties.dlx().name())
                .withArgument("x-dead-letter-routing-key", properties.dlq().sales())
                .build();
    }

    @Bean
    public Queue mailQueue() {
        return QueueBuilder.durable(properties.queues().mail())
                .withArgument("x-dead-letter-exchange", properties.dlx().name())
                .withArgument("x-dead-letter-routing-key", properties.dlq().mail())
                .build();
    }

    // --- Main queue bindings ---

    @Bean
    public Binding salesBinding(Queue salesQueue, FanoutExchange salesFanoutExchange) {
        return BindingBuilder.bind(salesQueue).to(salesFanoutExchange);
    }

    @Bean
    public Binding mailBinding(Queue mailQueue, FanoutExchange salesFanoutExchange) {
        return BindingBuilder.bind(mailQueue).to(salesFanoutExchange);
    }
}
