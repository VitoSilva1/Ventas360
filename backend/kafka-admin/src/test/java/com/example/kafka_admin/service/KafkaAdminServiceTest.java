package com.example.kafka_admin.service;

import com.example.kafka_admin.dto.ConsumerGroupDetails;
import com.example.kafka_admin.dto.CreateTopicRequest;
import com.example.kafka_admin.dto.ResetOffsetsRequest;
import com.example.kafka_admin.dto.TopicDetails;
import com.example.kafka_admin.dto.UpdateTopicConfigRequest;
import com.example.kafka_admin.exception.KafkaAdminOperationException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1)
class KafkaAdminServiceTest {

    @Autowired
    private KafkaAdminService service;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void clusterInfo_reportsBrokers() {
        assertThat(service.clusterInfo().brokers()).hasSize(1);
    }

    @Test
    void createTopic_withPartitionsReplicationAndConfigs_isListedAndDescribed() {
        service.createTopic(new CreateTopicRequest("admin.test.create", 3, 1,
                Map.of("retention.ms", "3600000", "cleanup.policy", "delete")));

        assertThat(service.listTopics()).anySatisfy(t -> {
            assertThat(t.name()).isEqualTo("admin.test.create");
            assertThat(t.partitions()).isEqualTo(3);
            assertThat(t.replicationFactor()).isEqualTo(1);
        });
        TopicDetails details = service.describeTopic("admin.test.create");
        assertThat(details.partitions()).hasSize(3);
        assertThat(details.configs()).containsEntry("retention.ms", "3600000");
    }

    @Test
    void createTopic_alreadyExists_isConflict() {
        service.createTopic(new CreateTopicRequest("admin.test.dup", 1, 1, null));

        assertThatThrownBy(() -> service.createTopic(new CreateTopicRequest("admin.test.dup", 1, 1, null)))
                .isInstanceOfSatisfying(KafkaAdminOperationException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createTopic_replicationAboveBrokerCount_isBadRequest() {
        assertThatThrownBy(() -> service.createTopic(new CreateTopicRequest("admin.test.rf", 1, 3, null)))
                .isInstanceOfSatisfying(KafkaAdminOperationException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateConfigs_setsAndDeletesTopicOverrides() {
        service.createTopic(new CreateTopicRequest("admin.test.config", 1, 1, Map.of("retention.ms", "1000")));

        service.updateTopicConfigs("admin.test.config", new UpdateTopicConfigRequest(
                Map.of("cleanup.policy", "compact", "min.compaction.lag.ms", "60000"), List.of("retention.ms")));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Map<String, String> configs = service.describeTopic("admin.test.config").configs();
            assertThat(configs).containsEntry("cleanup.policy", "compact")
                    .containsEntry("min.compaction.lag.ms", "60000")
                    .doesNotContainKey("retention.ms");
        });
    }

    @Test
    void increasePartitions_growsTopicAndRejectsShrinking() {
        service.createTopic(new CreateTopicRequest("admin.test.partitions", 2, 1, null));

        service.increasePartitions("admin.test.partitions", 4);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(service.describeTopic("admin.test.partitions").partitions()).hasSize(4));
        assertThatThrownBy(() -> service.increasePartitions("admin.test.partitions", 1))
                .isInstanceOfSatisfying(KafkaAdminOperationException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteTopic_removesItAndProtectsInternalAndMissingTopics() {
        service.createTopic(new CreateTopicRequest("admin.test.delete", 1, 1, null));

        service.deleteTopic("admin.test.delete");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(service.listTopics()).noneMatch(t -> t.name().equals("admin.test.delete")));
        assertThatThrownBy(() -> service.deleteTopic("__consumer_offsets"))
                .isInstanceOfSatisfying(KafkaAdminOperationException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.deleteTopic("admin.test.never-created"))
                .isInstanceOfSatisfying(KafkaAdminOperationException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void internalTopics_cannotBeCreatedReconfiguredOrRepartitioned() {
        // Regression (T5 verification A1/A6): only delete was guarded before.
        assertBadRequest(() -> service.updateTopicConfigs("__consumer_offsets",
                new UpdateTopicConfigRequest(Map.of("retention.ms", "1000"), null)));
        assertBadRequest(() -> service.increasePartitions("__consumer_offsets", 60));
        assertBadRequest(() -> service.createTopic(new CreateTopicRequest("__shadow", 1, 1, null)));

        assertThat(service.listTopics()).noneMatch(t -> t.name().equals("__shadow"));
    }

    private static void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(KafkaAdminOperationException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void consumerGroup_reportsLagResetsOffsetsAndCanBeDeleted() {
        String topic = "admin.test.lag";
        String group = "admin-test-group";
        service.createTopic(new CreateTopicRequest(topic, 1, 1, null));
        produce(topic, 5);
        commitOffset(group, new TopicPartition(topic, 0), 2);

        ConsumerGroupDetails details = service.describeConsumerGroup(group);
        assertThat(details.totalLag()).isEqualTo(3);
        assertThat(service.listConsumerGroups()).anySatisfy(g -> assertThat(g.groupId()).isEqualTo(group));

        service.resetOffsets(group, new ResetOffsetsRequest(topic, ResetOffsetsRequest.Strategy.EARLIEST));
        assertThat(service.describeConsumerGroup(group).totalLag()).isEqualTo(5);

        service.deleteConsumerGroup(group);
        assertThat(service.listConsumerGroups()).noneMatch(g -> g.groupId().equals(group));
    }

    @Test
    void describeConsumerGroup_unknown_isNotFound() {
        assertThatThrownBy(() -> service.describeConsumerGroup("admin-test-unknown-group"))
                .isInstanceOfSatisfying(KafkaAdminOperationException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void produce(String topic, int count) {
        var factory = new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(broker),
                new StringSerializer(), new StringSerializer());
        try {
            var template = new KafkaTemplate<>(factory);
            for (int i = 0; i < count; i++) {
                template.send(new ProducerRecord<>(topic, "k" + i, "v" + i)).join();
            }
        } finally {
            factory.destroy();
        }
    }

    private void commitOffset(String group, TopicPartition partition, long offset) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, group, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.assign(List.of(partition));
            consumer.commitSync(Map.of(partition, new OffsetAndMetadata(offset)));
        }
    }
}
