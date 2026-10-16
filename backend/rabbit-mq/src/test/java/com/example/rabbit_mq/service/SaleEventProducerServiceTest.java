package com.example.rabbit_mq.service;

import com.example.rabbit_mq.dto.SaleEventDto;
import com.example.rabbit_mq.dto.SaleEventItemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaleEventProducerServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SaleEventProducerService saleEventProducerService;

    private SaleEventDto validEvent() {
        return new SaleEventDto(
            UUID.randomUUID(), 2L, "customer@example.com",
            LocalDateTime.now(),
            List.of(new SaleEventItemDto(10L, "Product A", 2, new BigDecimal("75.00")))
        );
    }

    @Test
    void publish_sendsToFanoutExchange() {
        saleEventProducerService.publish(validEvent());
        verify(rabbitTemplate).convertAndSend(eq("ventas360.sales.fanout"), eq(""), any(SaleEventDto.class),
            any(org.springframework.amqp.core.MessagePostProcessor.class));
    }

    @Test
    void publish_returnsNonNullEventId() {
        String eventId = saleEventProducerService.publish(validEvent());
        assertThat(eventId).isNotNull().isNotBlank();
    }

    @Test
    void publish_returnsDifferentEventIdEachTime() {
        String id1 = saleEventProducerService.publish(validEvent());
        String id2 = saleEventProducerService.publish(validEvent());
        assertThat(id1).isNotEqualTo(id2);
    }
}
