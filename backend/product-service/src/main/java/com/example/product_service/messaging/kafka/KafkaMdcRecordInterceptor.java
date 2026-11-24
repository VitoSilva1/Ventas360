package com.example.product_service.messaging.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adds the coordinates of the record being processed to the logging MDC, so every log line written
 * while handling it (listener, validation, inventory service) carries kafka.topic/partition/offset/key
 * as structured ECS fields. Picked up automatically by Spring Boot's listener container factory.
 */
@Component
public class KafkaMdcRecordInterceptor implements RecordInterceptor<Object, Object> {

    static final String TOPIC = "kafka.topic";
    static final String PARTITION = "kafka.partition";
    static final String OFFSET = "kafka.offset";
    static final String KEY = "kafka.key";
    private static final List<String> KEYS = List.of(TOPIC, PARTITION, OFFSET, KEY);

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                    Consumer<Object, Object> consumer) {
        MDC.put(TOPIC, record.topic());
        MDC.put(PARTITION, String.valueOf(record.partition()));
        MDC.put(OFFSET, String.valueOf(record.offset()));
        MDC.put(KEY, String.valueOf(record.key()));
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        KEYS.forEach(MDC::remove);
    }
}
