package com.example.rabbit_mq.controller;

import com.example.rabbit_mq.dto.*;
import com.example.rabbit_mq.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<QueueResponse> createQueue(@Valid @RequestBody QueueRequest request) {
        boolean durable = request.durable() != null ? request.durable() : true;
        rabbitAdminService.createQueue(request.name(), durable);
        return ResponseEntity.status(201).body(new QueueResponse(request.name(), durable));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
        rabbitAdminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeResponse> createExchange(@Valid @RequestBody ExchangeRequest request) {
        boolean durable = request.durable() != null ? request.durable() : true;
        rabbitAdminService.createExchange(request.name(), request.type(), durable);
        return ResponseEntity.status(201).body(new ExchangeResponse(request.name(), request.type(), durable));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
        rabbitAdminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }
}
