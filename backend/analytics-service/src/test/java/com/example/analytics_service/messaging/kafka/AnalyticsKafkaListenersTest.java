package com.example.analytics_service.messaging.kafka;

import com.example.analytics_service.service.AnalyticsService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 3, topics = {
        AnalyticsKafkaListenersTest.SALES_TOPIC, AnalyticsKafkaListenersTest.SALES_DLT,
        AnalyticsKafkaListenersTest.PRODUCTS_TOPIC, AnalyticsKafkaListenersTest.PRODUCTS_DLT})
@DirtiesContext
class AnalyticsKafkaListenersTest {

    static final String SALES_TOPIC = "ventas360.sales.completed";
    static final String SALES_DLT = "ventas360.sales.completed.DLT";
    static final String PRODUCTS_TOPIC = "ventas360.product.events";
    static final String PRODUCTS_DLT = "ventas360.product.events.DLT";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    void validEvents_areConsumedFromBothTopics() {
        send(PRODUCTS_TOPIC, "77", """
                {"eventId":"6f1c1f7e-0000-4000-8000-000000000077","type":"CREATED","productId":77,
                 "name":"Parlante","price":15000,"stock":2,"active":true,"occurredAt":"2026-11-23T10:00:00Z"}""");
        send(SALES_TOPIC, "500", saleJson(500, 77));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var summary = analyticsService.summary();
            assertThat(summary.topProducts()).anySatisfy(top -> {
                assertThat(top.productId()).isEqualTo(77L);
                assertThat(top.name()).isEqualTo("Parlante");
            });
            assertThat(summary.lowStock()).anySatisfy(p -> assertThat(p.productId()).isEqualTo(77L));
        });
    }

    @Test
    void malformedJson_goesToDeadLetterTopicAndDoesNotBlockLaterEvents() {
        send(SALES_TOPIC, "601", "{not-json");
        send(SALES_TOPIC, "601", saleJson(601, 1));

        ConsumerRecord<String, String> dead = readFrom(SALES_DLT, "601");
        assertThat(dead.value()).isEqualTo("{not-json");
        assertThat(dead.headers().lastHeader("kafka_dlt-exception-message")).isNotNull();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(analyticsService.isSaleRecorded(601L)).isTrue());
    }

    @Test
    void invalidPayload_goesToDeadLetterTopicWithoutBeingRecorded() {
        // Valid JSON, but breaks the contract: a sale without items.
        send(SALES_TOPIC, "602", """
                {"saleId":602,"total":10,"items":[]}""");

        ConsumerRecord<String, String> dead = readFrom(SALES_DLT, "602");
        assertThat(dead.headers().lastHeader("kafka_dlt-exception-fqcn")).isNotNull();
        assertThat(analyticsService.isSaleRecorded(602L)).isFalse();
    }

    private static String saleJson(long saleId, long productId) {
        return """
                {"eventId":"6f1c1f7e-0000-4000-8000-%012d","saleId":%d,"saleEventId":"evt-%d","customerId":1,
                 "total":3000,"status":"CREATED","createdAt":"2026-11-23T10:00:00","publishedAt":"2026-11-23T10:00:01Z",
                 "items":[{"productId":%d,"quantity":3,"unitPrice":1000,"subtotal":3000}]}"""
                .formatted(saleId, saleId, saleId, productId);
    }

    private void send(String topic, String key, String json) {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        var producerFactory = new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer());
        try {
            new KafkaTemplate<>(producerFactory).send(new ProducerRecord<>(topic, key, json)).join();
        } finally {
            producerFactory.destroy();
        }
    }

    private ConsumerRecord<String, String> readFrom(String topic, String key) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "dlt-reader-" + key, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, topic);
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("No record with key " + key + " in " + topic);
    }
}
