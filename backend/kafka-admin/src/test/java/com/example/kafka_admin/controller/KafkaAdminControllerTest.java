package com.example.kafka_admin.controller;

import com.example.kafka_admin.dto.CreateTopicRequest;
import com.example.kafka_admin.exception.KafkaAdminOperationException;
import com.example.kafka_admin.service.KafkaAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KafkaAdminControllerTest {

    private KafkaAdminService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(KafkaAdminService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new KafkaAdminController(service))
                .setControllerAdvice(new KafkaAdminExceptionHandler())
                .build();
    }

    @Test
    void createTopic_valid_returns201() throws Exception {
        mockMvc.perform(post("/kafka-admin/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"orders.v1","partitions":3,"replicationFactor":3,
                                 "configs":{"retention.ms":"86400000"}}"""))
                .andExpect(status().isCreated());

        verify(service).createTopic(any(CreateTopicRequest.class));
    }

    @Test
    void createTopic_invalidBody_returns400WithoutCallingKafka() throws Exception {
        mockMvc.perform(post("/kafka-admin/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"bad name!","partitions":0,"replicationFactor":1}"""))
                .andExpect(status().isBadRequest());

        verify(service, never()).createTopic(any());
    }

    @Test
    void createTopic_conflictFromKafka_returns409ProblemDetail() throws Exception {
        doThrow(new KafkaAdminOperationException(HttpStatus.CONFLICT, "Topic 'orders.v1' already exists"))
                .when(service).createTopic(any());

        mockMvc.perform(post("/kafka-admin/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"orders.v1","partitions":1,"replicationFactor":1}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Topic 'orders.v1' already exists"));
    }

    @Test
    void updateConfigs_emptyBody_returns400() throws Exception {
        mockMvc.perform(patch("/kafka-admin/topics/orders.v1/configs").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTopic_returns204() throws Exception {
        mockMvc.perform(delete("/kafka-admin/topics/orders.v1"))
                .andExpect(status().isNoContent());

        verify(service).deleteTopic(eq("orders.v1"));
    }

    @Test
    void resetOffsets_activeGroup_returns409() throws Exception {
        doThrow(new KafkaAdminOperationException(HttpStatus.CONFLICT, "Consumer group 'g' is not empty"))
                .when(service).resetOffsets(eq("g"), any());

        mockMvc.perform(post("/kafka-admin/consumer-groups/g/offsets/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic":"orders.v1","strategy":"EARLIEST"}"""))
                .andExpect(status().isConflict());
    }
}
