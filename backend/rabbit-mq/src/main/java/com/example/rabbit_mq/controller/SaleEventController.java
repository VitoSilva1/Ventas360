package com.example.rabbit_mq.controller;

import com.example.rabbit_mq.dto.SaleEventDto;
import com.example.rabbit_mq.dto.SaleEventResponse;
import com.example.rabbit_mq.service.SaleEventProducerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaleEventController {

    private final SaleEventProducerService producerService;
    private final String exchangeName;

    public SaleEventController(SaleEventProducerService producerService,
                                @Value("${rabbitmq.ventas360.exchange.sales-fanout}") String exchangeName) {
        this.producerService = producerService;
        this.exchangeName = exchangeName;
    }

    @PostMapping("/sales/events")
    public ResponseEntity<SaleEventResponse> publishSaleEvent(@Valid @RequestBody SaleEventDto event) {
        String eventId = producerService.publish(event);
        return ResponseEntity.accepted().body(new SaleEventResponse(eventId, exchangeName));
    }
}
