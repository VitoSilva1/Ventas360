package com.example.product_service.messaging.kafka;

import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.service.ProductService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 3, topics = ProductEventsKafkaRelayTest.TOPIC)
class ProductEventsKafkaRelayTest {

    static final String TOPIC = "ventas360.product.events";

    @Autowired
    private ProductService productService;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void createdProduct_isPublishedAfterCommitKeyedByProductId() {
        ProductResponse created = productService.create(
                new ProductRequest("Teclado", "Mecanico", new BigDecimal("39990"), 7, true));

        ConsumerRecord<String, String> record = readSingleRecord();
        assertThat(record.key()).isEqualTo(String.valueOf(created.id()));
        assertThat(record.value())
                .contains("\"type\":\"CREATED\"")
                .contains("\"productId\":" + created.id())
                .contains("\"name\":\"Teclado\"")
                .contains("\"stock\":7");
        assertThat(record.headers().lastHeader("__TypeId__")).isNull();
    }

    private ConsumerRecord<String, String> readSingleRecord() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "relay-test", false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
            return KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10));
        }
    }
}
