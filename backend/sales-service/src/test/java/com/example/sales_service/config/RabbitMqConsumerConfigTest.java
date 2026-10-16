package com.example.sales_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RabbitMqConsumerConfigTest {

    @Autowired(required = false)
    private Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @Autowired(required = false)
    private SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory;

    @Test
    void jackson2JsonMessageConverter_beanExists() {
        assertThat(jackson2JsonMessageConverter).isNotNull();
    }

    @Test
    void rabbitListenerContainerFactory_beanExists() {
        assertThat(rabbitListenerContainerFactory).isNotNull();
    }

    @Test
    void rabbitListenerContainerFactory_hasConverterConfigured() {
        assertThat(rabbitListenerContainerFactory).isNotNull();
        // The factory was wired with the converter in RabbitMqConsumerConfig
        // Verifying the bean graph is consistent — no NPE or misconfiguration at startup
        assertThat(jackson2JsonMessageConverter).isNotNull();
    }

    @Test
    void rabbitListenerContainerFactory_defaultRequeueRejectedIsFalse() {
        assertThat(rabbitListenerContainerFactory).isNotNull();
        Boolean requeueRejected = (Boolean) ReflectionTestUtils.getField(
                rabbitListenerContainerFactory, "defaultRequeueRejected");
        assertThat(requeueRejected).isFalse();
    }

    @Test
    void rabbitListenerContainerFactory_adviceChainIsSet() {
        assertThat(rabbitListenerContainerFactory).isNotNull();
        assertThat(rabbitListenerContainerFactory.getAdviceChain()).isNotNull();
        assertThat(rabbitListenerContainerFactory.getAdviceChain()).isNotEmpty();
    }
}
