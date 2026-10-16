package com.example.rabbit_mq.service;

import com.example.rabbit_mq.dto.SaleEventDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SaleEventProducerService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.ventas360.exchange.sales-fanout}")
    private String exchangeName = "ventas360.sales.fanout";

    @Autowired
    public SaleEventProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public String publish(SaleEventDto event) {
        String eventId = UUID.randomUUID().toString();
        rabbitTemplate.convertAndSend(exchangeName, "", event,
            m -> { m.getMessageProperties().setMessageId(eventId); return m; });
        return eventId;
    }
}
