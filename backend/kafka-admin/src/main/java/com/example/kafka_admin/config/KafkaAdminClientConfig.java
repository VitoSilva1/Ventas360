package com.example.kafka_admin.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class KafkaAdminClientConfig {

    /**
     * Long-lived Kafka Admin client shared by every request; closed on shutdown.
     */
    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return Admin.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.CLIENT_ID_CONFIG, "kafka-admin-service",
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 15_000));
    }
}
