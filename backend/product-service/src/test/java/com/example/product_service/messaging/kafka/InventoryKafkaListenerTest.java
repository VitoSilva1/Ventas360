package com.example.product_service.messaging.kafka;

import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true"})
@EmbeddedKafka(partitions = 3, topics = {
        InventoryKafkaListenerTest.SALES_TOPIC, InventoryKafkaListenerTest.SALES_DLT,
        "ventas360.product.events"})
@DirtiesContext
class InventoryKafkaListenerTest {

    static final String SALES_TOPIC = "ventas360.sales.completed";
    static final String SALES_DLT = "ventas360.sales.completed.DLT";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void saleEvent_decrementsStockThroughInventoryGroup() {
        Long id = productRepository.save(new Product("Audifonos", null, new BigDecimal("20000"), 10, true)).getId();

        send("900", """
                {"eventId":"6f1c1f7e-0000-4000-8000-000000000900","saleId":900,"total":40000,
                 "items":[{"productId":%d,"quantity":2,"unitPrice":20000,"subtotal":40000}]}""".formatted(id));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(productRepository.findById(id).orElseThrow().getStock()).isEqualTo(8));
    }

    @Test
    void invalidSaleEvent_goesToDeadLetterTopic() {
        send("901", """
                {"saleId":901,"items":[]}""");

        ConsumerRecord<String, String> dead = readFrom(SALES_DLT, "901");
        assertThat(dead.headers().lastHeader("kafka_dlt-exception-fqcn")).isNotNull();
    }

    private void send(String key, String json) {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        var producerFactory = new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer());
        try {
            new KafkaTemplate<>(producerFactory).send(new ProducerRecord<>(SALES_TOPIC, key, json)).join();
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
