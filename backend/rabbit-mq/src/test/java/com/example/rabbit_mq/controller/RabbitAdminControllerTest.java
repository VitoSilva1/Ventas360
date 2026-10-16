package com.example.rabbit_mq.controller;

import com.example.rabbit_mq.exception.RabbitResourceNotFoundException;
import com.example.rabbit_mq.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RabbitAdminController.class)
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitAdminService rabbitAdminService;

    @Test
    void createQueue_valid_returns201() throws Exception {
        mockMvc.perform(post("/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"my-queue\",\"durable\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("my-queue"));
    }

    @Test
    void createQueue_blankName_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"durable\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void createQueue_nameTooLong_returns400() throws Exception {
        String longName = "a".repeat(256);
        mockMvc.perform(post("/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + longName + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createQueue_invalidChars_returns400() throws Exception {
        mockMvc.perform(post("/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"invalid name!\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteQueue_existing_returns204() throws Exception {
        mockMvc.perform(delete("/queues/my-queue"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteQueue_missing_returns404ProblemDetail() throws Exception {
        doThrow(new RabbitResourceNotFoundException("Queue 'missing' not found"))
            .when(rabbitAdminService).deleteQueue("missing");
        mockMvc.perform(delete("/queues/missing"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void createExchange_validFanout_returns201() throws Exception {
        mockMvc.perform(post("/exchanges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"my-exchange\",\"type\":\"fanout\",\"durable\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("my-exchange"))
            .andExpect(jsonPath("$.type").value("fanout"));
    }

    @Test
    void createExchange_invalidType_returns400() throws Exception {
        mockMvc.perform(post("/exchanges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"my-exchange\",\"type\":\"invalid\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createExchange_blankName_returns400() throws Exception {
        mockMvc.perform(post("/exchanges")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"type\":\"fanout\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteExchange_returns204() throws Exception {
        mockMvc.perform(delete("/exchanges/my-exchange"))
            .andExpect(status().isNoContent());
    }
}
