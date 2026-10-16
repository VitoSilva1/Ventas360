package com.example.rabbit_mq;

import com.example.rabbit_mq.config.RabbitMqTopologyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RabbitMqApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RabbitMqTopologyProperties properties;

    @Test
    void contextLoads() {
    }

    @Test
    void topologyBeanCounts() {
        Map<String, FanoutExchange> fanoutExchanges = context.getBeansOfType(FanoutExchange.class);
        Map<String, DirectExchange> directExchanges = context.getBeansOfType(DirectExchange.class);
        Map<String, Queue> queues = context.getBeansOfType(Queue.class);
        Map<String, Binding> bindings = context.getBeansOfType(Binding.class);

        assertThat(fanoutExchanges).hasSize(1);
        assertThat(directExchanges).hasSize(1);
        assertThat(queues).hasSize(4);
        assertThat(bindings).hasSize(4);
    }

    @Test
    void topologyNamesMatchYaml() {
        Map<String, FanoutExchange> exchanges = context.getBeansOfType(FanoutExchange.class);
        Map<String, Queue> queues = context.getBeansOfType(Queue.class);

        assertThat(exchanges.values())
            .extracting(FanoutExchange::getName)
            .containsExactly(properties.exchange().salesFanout());

        assertThat(queues.values())
            .extracting(Queue::getName)
            .containsExactlyInAnyOrder(
                    properties.queues().sales(),
                    properties.queues().mail(),
                    properties.dlq().sales(),
                    properties.dlq().mail());
    }
}
