package com.example.kafka_admin.dto;

public record TopicSummary(String name, int partitions, int replicationFactor) {
}
