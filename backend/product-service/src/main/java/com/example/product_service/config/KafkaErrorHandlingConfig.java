package com.example.product_service.config;

import com.example.product_service.messaging.kafka.InvalidEventException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error strategy for the inventory listener:
 * transient failures are retried with exponential backoff (0.5s, 1s, 2s);
 * invalid payloads and undeserializable messages skip the retries;
 * whatever still fails is published to {@code <topic>.DLT} (same partition) and logged.
 * <p>
 * The dead-letter producer is private to this class on purpose: exposing another KafkaTemplate
 * bean would replace the auto-configured one used by {@code ProductEventsKafkaRelay}.
 */
@Configuration
public class KafkaErrorHandlingConfig implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);
    public static final String DLT_SUFFIX = ".DLT";

    private final DefaultKafkaProducerFactory<String, Object> deadLetterProducerFactory;

    public KafkaErrorHandlingConfig(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "product-service-dlt");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);

        // Undeserializable records arrive as raw bytes; valid-but-rejected ones as DTOs.
        Map<Class<?>, Serializer<?>> byType = new LinkedHashMap<>();
        byType.put(byte[].class, new ByteArraySerializer());
        byType.put(Object.class, new JacksonJsonSerializer<>().noTypeInfo());
        this.deadLetterProducerFactory = new DefaultKafkaProducerFactory<>(props, new StringSerializer(),
                new DelegatingByTypeSerializer(byType, true));
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // Explicit destination: Spring Kafka 4 defaults to a "-dlt" suffix, but our topics are
        // pre-created as "<topic>.DLT" (auto topic creation is disabled on the cluster).
        var deadLetterPublisher = new DeadLetterPublishingRecoverer(new KafkaTemplate<>(deadLetterProducerFactory),
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition()));

        ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler handler = new DefaultErrorHandler((record, exception) -> {
            log.error("Event sent to DLT topic={} partition={} offset={} key={} error={}",
                    record.topic(), record.partition(), record.offset(), record.key(),
                    rootMessage(exception));
            deadLetterPublisher.accept(record, exception);
        }, backOff);

        handler.addNotRetryableExceptions(InvalidEventException.class);
        handler.setRetryListeners((record, exception, attempt) ->
                log.warn("Event processing failed, retrying topic={} partition={} offset={} key={} attempt={} error={}",
                        record.topic(), record.partition(), record.offset(), record.key(), attempt,
                        rootMessage(exception)));
        return handler;
    }

    @Override
    public void destroy() {
        deadLetterProducerFactory.destroy();
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
