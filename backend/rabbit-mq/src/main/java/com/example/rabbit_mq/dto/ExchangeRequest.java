package com.example.rabbit_mq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExchangeRequest(
    @NotBlank(message = "Exchange name is required")
    @Size(max = 255, message = "Exchange name must not exceed 255 characters")
    String name,
    @NotBlank(message = "Exchange type is required")
    @Pattern(regexp = "fanout|direct|topic|headers", message = "Exchange type must be fanout, direct, topic, or headers")
    String type,
    Boolean durable
) {}
