package com.example.sales_service.messaging.kafka;

import com.example.sales_service.dto.SaleCompletedEvent;
import com.example.sales_service.dto.SaleItemResponse;
import com.example.sales_service.dto.SaleResponse;
import com.example.sales_service.model.SaleStatus;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 3, topics = SaleCompletedPublisherTest.TOPIC)
class SaleCompletedPublisherTest {

    static final String TOPIC = "ventas360.sales.completed";

    @Autowired
    private SaleCompletedPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void publishAndWait_sendsJsonEventKeyedBySaleIdWithoutTypeHeaders() {
        SaleResponse sale = new SaleResponse(42L, 7L, new BigDecimal("30.00"), SaleStatus.CREATED,
                LocalDateTime.of(2026, 11, 23, 10, 0),
                List.of(new SaleItemResponse(1L, 10L, 3, new BigDecimal("10.00"), new BigDecimal("30.00"))));

        publisher.publishAndWait(SaleCompletedEvent.from(sale, "evt-123"));

        ConsumerRecord<String, String> record = readSingleRecord();
        assertThat(record.key()).isEqualTo("42");
        assertThat(record.value())
                .contains("\"saleId\":42")
                .contains("\"saleEventId\":\"evt-123\"")
                .contains("\"customerId\":7")
                .contains("\"productId\":10")
                .contains("\"quantity\":3");
        assertThat(record.headers().lastHeader("__TypeId__")).isNull();
    }

    private ConsumerRecord<String, String> readSingleRecord() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "publisher-test", false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
            return KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10));
        }
    }
}
