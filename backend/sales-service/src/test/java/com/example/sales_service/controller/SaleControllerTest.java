package com.example.sales_service.controller;

import com.example.sales_service.dto.SaleCompletedEvent;
import com.example.sales_service.dto.SaleItemRequest;
import com.example.sales_service.dto.SaleRequest;
import com.example.sales_service.dto.SaleResponse;
import com.example.sales_service.messaging.kafka.SaleCompletedPublisher;
import com.example.sales_service.service.SaleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleControllerTest {

    @Mock
    private SaleService saleService;

    @Mock
    private SaleCompletedPublisher saleCompletedPublisher;

    @InjectMocks
    private SaleController saleController;

    @Test
    void create_persistsSaleAndPublishesItAsynchronously() {
        SaleRequest request = new SaleRequest(1L, List.of(new SaleItemRequest(10L, 1, BigDecimal.TEN)));
        SaleResponse created = new SaleResponse(5L, 1L, BigDecimal.TEN, null, LocalDateTime.now(), List.of());
        when(saleService.create(request)).thenReturn(created);

        var response = saleController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
        ArgumentCaptor<SaleCompletedEvent> captor = ArgumentCaptor.forClass(SaleCompletedEvent.class);
        verify(saleCompletedPublisher).publishAsync(captor.capture());
        assertThat(captor.getValue().saleId()).isEqualTo(5L);
        assertThat(captor.getValue().saleEventId()).isNull();
    }
}
