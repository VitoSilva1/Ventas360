package com.example.rabbit_mq.testconfig;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestRabbitAdminConfig {

    @Bean
    @ConditionalOnMissingBean(RabbitAdmin.class)
    public RabbitAdmin rabbitAdmin() {
        return Mockito.mock(RabbitAdmin.class);
    }

    @Bean
    @ConditionalOnMissingBean(ConnectionFactory.class)
    public ConnectionFactory connectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }
}
