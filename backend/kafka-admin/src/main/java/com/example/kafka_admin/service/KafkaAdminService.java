package com.example.kafka_admin.service;

import com.example.kafka_admin.dto.ClusterInfo;
import com.example.kafka_admin.dto.ConsumerGroupDetails;
import com.example.kafka_admin.dto.ConsumerGroupSummary;
import com.example.kafka_admin.dto.CreateTopicRequest;
import com.example.kafka_admin.dto.ResetOffsetsRequest;
import com.example.kafka_admin.dto.TopicDetails;
import com.example.kafka_admin.dto.TopicSummary;
import com.example.kafka_admin.dto.UpdateTopicConfigRequest;
import com.example.kafka_admin.exception.KafkaAdminOperationException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.RebalanceInProgressException;
import org.apache.kafka.common.errors.UnknownMemberIdException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Management operations over the Kafka cluster through the Kafka {@link Admin} client.
 * Every call is bounded by a timeout and Kafka errors are translated to HTTP-friendly statuses.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private final Admin admin;
    private final Duration timeout;

    public KafkaAdminService(Admin admin, @Value("${ventas360.kafka-admin.timeout:10s}") Duration timeout) {
        this.admin = admin;
        this.timeout = timeout;
    }

    // ---------------------------------------------------------------- cluster

    public ClusterInfo clusterInfo() {
        var cluster = admin.describeCluster();
        List<ClusterInfo.Broker> brokers = await(cluster.nodes(), "describe cluster").stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new ClusterInfo.Broker(n.id(), n.host(), n.port()))
                .toList();
        Node controller = await(cluster.controller(), "describe cluster");
        return new ClusterInfo(await(cluster.clusterId(), "describe cluster"),
                controller == null ? null : controller.id(), brokers);
    }

    // ---------------------------------------------------------------- topics

    public List<TopicSummary> listTopics() {
        Collection<String> names = await(admin.listTopics(new ListTopicsOptions().listInternal(false)).names(),
                "list topics");
        if (names.isEmpty()) {
            return List.of();
        }
        return await(admin.describeTopics(names).allTopicNames(), "describe topics").values().stream()
                .map(KafkaAdminService::toSummary)
                .sorted(Comparator.comparing(TopicSummary::name))
                .toList();
    }

    public TopicDetails describeTopic(String name) {
        TopicDescription description = await(admin.describeTopics(List.of(name)).allTopicNames(),
                "describe topic " + name).get(name);
        List<TopicDetails.Partition> partitions = description.partitions().stream()
                .map(p -> new TopicDetails.Partition(p.partition(),
                        p.leader() == null ? null : p.leader().id(),
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();
        return new TopicDetails(name, partitions, topicOverrides(name));
    }

    public void createTopic(CreateTopicRequest request) {
        requireNotInternal(request.name(), "created");
        NewTopic topic = new NewTopic(request.name(), request.partitions(), request.replicationFactor().shortValue());
        if (request.configs() != null && !request.configs().isEmpty()) {
            topic.configs(request.configs());
        }
        await(admin.createTopics(List.of(topic)).all(), "create topic " + request.name());
        log.info("Kafka topic created name={} partitions={} replicationFactor={} configs={}",
                request.name(), request.partitions(), request.replicationFactor(), request.configs());
    }

    public void updateTopicConfigs(String name, UpdateTopicConfigRequest request) {
        requireNotInternal(name, "reconfigured");
        List<AlterConfigOp> ops = new ArrayList<>();
        if (request.set() != null) {
            request.set().forEach((key, value) ->
                    ops.add(new AlterConfigOp(new ConfigEntry(key, value), AlterConfigOp.OpType.SET)));
        }
        if (request.delete() != null) {
            request.delete().forEach(key ->
                    ops.add(new AlterConfigOp(new ConfigEntry(key, null), AlterConfigOp.OpType.DELETE)));
        }
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        await(admin.incrementalAlterConfigs(Map.of(resource, ops)).all(), "update configs of topic " + name);
        log.info("Kafka topic configs updated name={} set={} delete={}", name, request.set(), request.delete());
    }

    public void increasePartitions(String name, int totalCount) {
        // Repartitioning __consumer_offsets would move every group's offsets to other partitions.
        requireNotInternal(name, "repartitioned");
        await(admin.createPartitions(Map.of(name, NewPartitions.increaseTo(totalCount))).all(),
                "increase partitions of topic " + name);
        log.info("Kafka topic partitions increased name={} totalCount={}", name, totalCount);
    }

    public void deleteTopic(String name) {
        requireNotInternal(name, "deleted");
        await(admin.deleteTopics(List.of(name)).all(), "delete topic " + name);
        log.warn("Kafka topic deleted name={}", name);
    }

    /**
     * Topics starting with "__" belong to Kafka itself (__consumer_offsets, __transaction_state):
     * this API never creates, reconfigures, repartitions or deletes them.
     */
    private static void requireNotInternal(String name, String action) {
        if (name.startsWith("__")) {
            throw new KafkaAdminOperationException(HttpStatus.BAD_REQUEST,
                    "Internal topic '" + name + "' cannot be " + action);
        }
    }

    // ---------------------------------------------------------------- consumer groups

    public List<ConsumerGroupSummary> listConsumerGroups() {
        return await(admin.listConsumerGroups().all(), "list consumer groups").stream()
                .map(g -> new ConsumerGroupSummary(g.groupId(),
                        g.groupState().map(GroupState::toString).orElse("UNKNOWN")))
                .sorted(Comparator.comparing(ConsumerGroupSummary::groupId))
                .toList();
    }

    public ConsumerGroupDetails describeConsumerGroup(String groupId) {
        ConsumerGroupDescription description = await(
                admin.describeConsumerGroups(List.of(groupId)).describedGroups().get(groupId),
                "describe consumer group " + groupId);
        Map<TopicPartition, OffsetAndMetadata> committed = committedOffsets(groupId);
        if (description.groupState() == GroupState.DEAD && committed.isEmpty()) {
            throw new KafkaAdminOperationException(HttpStatus.NOT_FOUND,
                    "Consumer group '" + groupId + "' does not exist");
        }

        List<ConsumerGroupDetails.Member> members = description.members().stream()
                .map(m -> new ConsumerGroupDetails.Member(m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream().map(TopicPartition::toString).sorted().toList()))
                .toList();

        Map<TopicPartition, Long> endOffsets = offsets(committed.keySet(), OffsetSpec.latest());
        List<ConsumerGroupDetails.PartitionLag> lags = committed.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> {
                    long end = endOffsets.getOrDefault(e.getKey(), e.getValue().offset());
                    long lag = Math.max(0, end - e.getValue().offset());
                    return new ConsumerGroupDetails.PartitionLag(e.getKey().topic(), e.getKey().partition(),
                            e.getValue().offset(), end, lag);
                })
                .sorted(Comparator.comparing(ConsumerGroupDetails.PartitionLag::topic)
                        .thenComparingInt(ConsumerGroupDetails.PartitionLag::partition))
                .toList();
        long totalLag = lags.stream().mapToLong(ConsumerGroupDetails.PartitionLag::lag).sum();

        return new ConsumerGroupDetails(groupId, description.groupState().toString(), members, lags, totalLag);
    }

    /**
     * Moves the committed offsets of an inactive group to the earliest or latest offset of a topic.
     * Kafka only allows this when no consumer of the group is running.
     */
    public void resetOffsets(String groupId, ResetOffsetsRequest request) {
        requireInactive(groupId);
        TopicDescription topic = await(admin.describeTopics(List.of(request.topic())).allTopicNames(),
                "describe topic " + request.topic()).get(request.topic());
        List<TopicPartition> partitions = topic.partitions().stream()
                .map(p -> new TopicPartition(request.topic(), p.partition()))
                .toList();
        OffsetSpec spec = request.strategy() == ResetOffsetsRequest.Strategy.EARLIEST
                ? OffsetSpec.earliest() : OffsetSpec.latest();
        Map<TopicPartition, OffsetAndMetadata> target = offsets(partitions, spec).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new OffsetAndMetadata(e.getValue())));
        await(admin.alterConsumerGroupOffsets(groupId, target).all(), "reset offsets of group " + groupId);
        log.warn("Consumer group offsets reset groupId={} topic={} strategy={}",
                groupId, request.topic(), request.strategy());
    }

    public void deleteConsumerGroup(String groupId) {
        await(admin.deleteConsumerGroups(List.of(groupId)).all(), "delete consumer group " + groupId);
        log.warn("Consumer group deleted groupId={}", groupId);
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, String> topicOverrides(String name) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config config = await(admin.describeConfigs(List.of(resource)).all(), "describe configs of " + name)
                .get(resource);
        return config.entries().stream()
                .filter(e -> e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value, (a, b) -> a, TreeMap::new));
    }

    private Map<TopicPartition, OffsetAndMetadata> committedOffsets(String groupId) {
        return await(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata(),
                "list offsets of group " + groupId);
    }

    private Map<TopicPartition, Long> offsets(Collection<TopicPartition> partitions, OffsetSpec spec) {
        if (partitions.isEmpty()) {
            return Map.of();
        }
        Map<TopicPartition, OffsetSpec> request = partitions.stream()
                .collect(Collectors.toMap(Function.identity(), p -> spec));
        return await(admin.listOffsets(request).all(), "list partition offsets").entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset()));
    }

    private void requireInactive(String groupId) {
        ConsumerGroupDescription description = await(
                admin.describeConsumerGroups(List.of(groupId)).describedGroups().get(groupId),
                "describe consumer group " + groupId);
        GroupState state = description.groupState();
        if (state != GroupState.EMPTY && state != GroupState.DEAD) {
            throw new KafkaAdminOperationException(HttpStatus.CONFLICT,
                    "Consumer group '" + groupId + "' is " + state + "; stop its consumers before resetting offsets");
        }
    }

    private static TopicSummary toSummary(TopicDescription description) {
        int replication = description.partitions().isEmpty() ? 0 : description.partitions().getFirst().replicas().size();
        return new TopicSummary(description.name(), description.partitions().size(), replication);
    }

    private <T> T await(KafkaFuture<T> future, String operation) {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException(HttpStatus.SERVICE_UNAVAILABLE, operation + " was interrupted", e);
        } catch (TimeoutException e) {
            throw new KafkaAdminOperationException(HttpStatus.SERVICE_UNAVAILABLE,
                    operation + " timed out after " + timeout.toSeconds() + "s", e);
        } catch (ExecutionException e) {
            throw translate(operation, e.getCause() == null ? e : e.getCause());
        }
    }

    private static KafkaAdminOperationException translate(String operation, Throwable cause) {
        HttpStatus status = switch (cause) {
            case TopicExistsException ignored -> HttpStatus.CONFLICT;
            case GroupNotEmptyException ignored -> HttpStatus.CONFLICT;
            // A consumer joined between the "group is inactive" check and the offset change.
            case UnknownMemberIdException ignored -> HttpStatus.CONFLICT;
            case RebalanceInProgressException ignored -> HttpStatus.CONFLICT;
            case UnknownTopicOrPartitionException ignored -> HttpStatus.NOT_FOUND;
            case GroupIdNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case InvalidPartitionsException ignored -> HttpStatus.BAD_REQUEST;
            case InvalidReplicationFactorException ignored -> HttpStatus.BAD_REQUEST;
            // Also covers InvalidTopicException, which extends it.
            case InvalidConfigurationException ignored -> HttpStatus.BAD_REQUEST;
            case InvalidRequestException ignored -> HttpStatus.BAD_REQUEST;
            case PolicyViolationException ignored -> HttpStatus.BAD_REQUEST;
            case org.apache.kafka.common.errors.TimeoutException ignored -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
        if (status == HttpStatus.BAD_GATEWAY) {
            log.error("Kafka admin operation failed operation='{}'", operation, cause);
        } else {
            log.warn("Kafka admin operation rejected operation='{}' status={} reason={}",
                    operation, status.value(), cause.getMessage());
        }
        return new KafkaAdminOperationException(status, operation + " failed: " + cause.getMessage(), cause);
    }
}
