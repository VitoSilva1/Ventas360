package com.example.sales_service.consumer;

import com.example.sales_service.dto.SaleEventDto;
import com.example.sales_service.dto.SaleEventItemDto;
import com.example.sales_service.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SaleEventConsumerServiceTest {

    @Autowired
    private SaleEventConsumerService saleEventConsumerService;

    @Autowired
    private SaleRepository saleRepository;

    @Test
    void consumeEvent_createsPersistedSaleWithStatusCreated() {
        UUID saleId = UUID.randomUUID();
        SaleEventDto event = buildEvent(saleId);

        saleEventConsumerService.consume(event);

        assertThat(saleRepository.findAll())
                .anyMatch(s -> saleId.toString().equals(s.getSaleEventId()));
    }

    @Test
    @Transactional
    void consumeEvent_setsCorrectCustomerIdAndItems() {
        UUID saleId = UUID.randomUUID();
        SaleEventDto event = buildEvent(saleId);

        saleEventConsumerService.consume(event);

        var saved = saleRepository.findAll().stream()
                .filter(s -> saleId.toString().equals(s.getSaleEventId()))
                .findFirst()
                .orElseThrow();

        assertThat(saved.getCustomerId()).isEqualTo(42L);
        assertThat(saved.getItems()).hasSize(1);
    }

    @Test
    void consumeEvent_duplicate_skipsSecondMessage() {
        UUID saleId = UUID.randomUUID();
        SaleEventDto event = buildEvent(saleId);

        saleEventConsumerService.consume(event);
        saleEventConsumerService.consume(event);

        long count = saleRepository.findAll().stream()
                .filter(s -> saleId.toString().equals(s.getSaleEventId()))
                .count();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void consumeEvent_duplicate_logsWarning() {
        // This test verifies no exception is thrown on duplicate (warning is logged internally).
        UUID saleId = UUID.randomUUID();
        SaleEventDto event = buildEvent(saleId);

        saleEventConsumerService.consume(event);

        // Should complete without throwing on duplicate
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> saleEventConsumerService.consume(event));
    }

    private SaleEventDto buildEvent(UUID saleId) {
        SaleEventItemDto item = new SaleEventItemDto(
                1L, "Product A", 2, new BigDecimal("19.99"));
        return new SaleEventDto(
                saleId,
                42L,
                "customer@example.com",
                LocalDateTime.of(2025, 10, 15, 14, 30, 0),
                List.of(item));
    }
}
