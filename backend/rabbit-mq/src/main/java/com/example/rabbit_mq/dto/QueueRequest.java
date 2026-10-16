package com.example.rabbit_mq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QueueRequest(
    @NotBlank(message = "Queue name is required")
    @Size(max = 255, message = "Queue name must not exceed 255 characters")
    @Pattern(regexp = "[\\w.:-]+", message = "Queue name contains invalid characters")
    String name,
    Boolean durable
) {}
