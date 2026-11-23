package com.example.kafka_admin.dto;

import java.util.List;
import java.util.Map;

/**
 * @param configs only the topic-level overrides (values that differ from broker defaults)
 */
public record TopicDetails(String name, List<Partition> partitions, Map<String, String> configs) {

    public record Partition(int partition, Integer leader, List<Integer> replicas, List<Integer> isr) {
    }
}
