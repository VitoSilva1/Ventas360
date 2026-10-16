package com.example.rabbit_mq.service;

import com.example.rabbit_mq.exception.RabbitResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private RabbitAdmin rabbitAdmin;

    @InjectMocks
    private RabbitAdminService rabbitAdminService;

    @Test
    void createQueue_declaresQueue() {
        rabbitAdminService.createQueue("test-queue", true);
        verify(rabbitAdmin).declareQueue(any(Queue.class));
    }

    @Test
    void deleteQueue_existing_deletesQueue() {
        when(rabbitAdmin.getQueueProperties("test-queue")).thenReturn(new Properties());
        rabbitAdminService.deleteQueue("test-queue");
        verify(rabbitAdmin).deleteQueue("test-queue");
    }

    @Test
    void deleteQueue_missing_throwsNotFoundException() {
        when(rabbitAdmin.getQueueProperties("missing")).thenReturn(null);
        assertThatThrownBy(() -> rabbitAdminService.deleteQueue("missing"))
            .isInstanceOf(RabbitResourceNotFoundException.class);
    }

    @Test
    void createExchange_fanout_declaresFanoutExchange() {
        rabbitAdminService.createExchange("test-exchange", "fanout", true);
        verify(rabbitAdmin).declareExchange(any(FanoutExchange.class));
    }

    @Test
    void createExchange_direct_declaresDirectExchange() {
        rabbitAdminService.createExchange("test-exchange", "direct", true);
        verify(rabbitAdmin).declareExchange(any(DirectExchange.class));
    }

    @Test
    void createExchange_topic_declaresTopicExchange() {
        rabbitAdminService.createExchange("test-exchange", "topic", true);
        verify(rabbitAdmin).declareExchange(any(TopicExchange.class));
    }

    @Test
    void createExchange_headers_declaresHeadersExchange() {
        rabbitAdminService.createExchange("test-exchange", "headers", true);
        verify(rabbitAdmin).declareExchange(any(HeadersExchange.class));
    }

    @Test
    void deleteExchange_invokesDelete() {
        rabbitAdminService.deleteExchange("test-exchange");
        verify(rabbitAdmin).deleteExchange("test-exchange");
    }
}
