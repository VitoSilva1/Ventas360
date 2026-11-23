package com.example.kafka_admin.controller;

import com.example.kafka_admin.dto.ClusterInfo;
import com.example.kafka_admin.dto.ConsumerGroupDetails;
import com.example.kafka_admin.dto.ConsumerGroupSummary;
import com.example.kafka_admin.dto.CreateTopicRequest;
import com.example.kafka_admin.dto.IncreasePartitionsRequest;
import com.example.kafka_admin.dto.ResetOffsetsRequest;
import com.example.kafka_admin.dto.TopicDetails;
import com.example.kafka_admin.dto.TopicSummary;
import com.example.kafka_admin.dto.UpdateTopicConfigRequest;
import com.example.kafka_admin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/kafka-admin")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/cluster")
    public ClusterInfo cluster() {
        return service.clusterInfo();
    }

    @GetMapping("/topics")
    public List<TopicSummary> listTopics() {
        return service.listTopics();
    }

    @GetMapping("/topics/{name}")
    public TopicDetails describeTopic(@PathVariable String name) {
        return service.describeTopic(name);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public void createTopic(@Valid @RequestBody CreateTopicRequest request) {
        service.createTopic(request);
    }

    @PatchMapping("/topics/{name}/configs")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTopicConfigs(@PathVariable String name, @Valid @RequestBody UpdateTopicConfigRequest request) {
        service.updateTopicConfigs(name, request);
    }

    @PostMapping("/topics/{name}/partitions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void increasePartitions(@PathVariable String name, @Valid @RequestBody IncreasePartitionsRequest request) {
        service.increasePartitions(name, request.totalCount());
    }

    @DeleteMapping("/topics/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(@PathVariable String name) {
        service.deleteTopic(name);
    }

    @GetMapping("/consumer-groups")
    public List<ConsumerGroupSummary> listConsumerGroups() {
        return service.listConsumerGroups();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupDetails describeConsumerGroup(@PathVariable String groupId) {
        return service.describeConsumerGroup(groupId);
    }

    @PostMapping("/consumer-groups/{groupId}/offsets/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetOffsets(@PathVariable String groupId, @Valid @RequestBody ResetOffsetsRequest request) {
        service.resetOffsets(groupId, request);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConsumerGroup(@PathVariable String groupId) {
        service.deleteConsumerGroup(groupId);
    }
}
