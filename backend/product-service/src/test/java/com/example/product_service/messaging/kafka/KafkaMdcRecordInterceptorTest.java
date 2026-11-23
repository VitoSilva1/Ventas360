package com.example.product_service.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaMdcRecordInterceptorTest {

    private final KafkaMdcRecordInterceptor interceptor = new KafkaMdcRecordInterceptor();
    private final ConsumerRecord<Object, Object> record =
            new ConsumerRecord<>("ventas360.sales.completed", 2, 41L, "500", "{}");

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void intercept_putsRecordCoordinatesInMdcAndKeepsTheRecord() {
        assertThat(interceptor.intercept(record, null)).isSameAs(record);

        assertThat(MDC.get("kafka.topic")).isEqualTo("ventas360.sales.completed");
        assertThat(MDC.get("kafka.partition")).isEqualTo("2");
        assertThat(MDC.get("kafka.offset")).isEqualTo("41");
        assertThat(MDC.get("kafka.key")).isEqualTo("500");
    }

    @Test
    void afterRecord_removesKafkaContextButKeepsUnrelatedMdcEntries() {
        MDC.put("traceId", "abc");
        interceptor.intercept(record, null);

        interceptor.afterRecord(record, null);

        assertThat(MDC.get("kafka.topic")).isNull();
        assertThat(MDC.get("kafka.offset")).isNull();
        assertThat(MDC.get("traceId")).isEqualTo("abc");
    }
}
