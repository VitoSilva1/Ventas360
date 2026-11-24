package com.example.kafka_admin.dto;

import java.util.List;

/**
 * @param totalLag sum of (end offset - committed offset) over every partition the group has committed
 */
public record ConsumerGroupDetails(
        String groupId,
        String state,
        List<Member> members,
        List<PartitionLag> offsets,
        long totalLag
) {
    public record Member(String clientId, String host, List<String> assignments) {
    }

    public record PartitionLag(String topic, int partition, long committedOffset, long endOffset, long lag) {
    }
}
