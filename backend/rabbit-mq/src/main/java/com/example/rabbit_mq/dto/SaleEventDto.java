package com.example.rabbit_mq.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleEventDto(
    @NotNull UUID saleId,
    Long customerId,
    String customerEmail,
    @NotNull LocalDateTime saleDate,
    @NotEmpty List<@Valid SaleEventItemDto> items
) {}
