package com.example.sales_service.consumer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.sales_service.dto.SaleEventDto;
import com.example.sales_service.dto.SaleEventItemDto;
import com.example.sales_service.dto.SaleResponse;
import com.example.sales_service.dto.SaleCompletedEvent;
import com.example.sales_service.messaging.kafka.KafkaPublishException;
import com.example.sales_service.messaging.kafka.SaleCompletedPublisher;
import com.example.sales_service.repository.SaleRepository;
import org.mockito.ArgumentCaptor;
import com.example.sales_service.service.SaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleEventConsumerServiceUnitTest {

    @Mock
    private SaleService saleService;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleCompletedPublisher saleCompletedPublisher;

    @InjectMocks
    private SaleEventConsumerService saleEventConsumerService;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger consumerLogger;

    @BeforeEach
    void setUpLogCapture() {
        consumerLogger = (Logger) LoggerFactory.getLogger(SaleEventConsumerService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        consumerLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDownLogCapture() {
        consumerLogger.detachAppender(listAppender);
    }

    @Test
    void consume_serviceThrows_propagatesException() {
        UUID saleId = UUID.randomUUID();
        SaleEventDto event = new SaleEventDto(
                saleId, 1L, "test@example.com",
                LocalDateTime.now(),
                List.of(new SaleEventItemDto(10L, "Product", 1, new BigDecimal("10.00")))
        );
        when(saleRepository.existsBySaleEventId(saleId.toString())).thenReturn(false);
        when(saleService.create(any())).thenThrow(new RuntimeException("DB unavailable"));

        assertThrows(RuntimeException.class, () -> saleEventConsumerService.consume(event));
    }

    @Test
    void consume_newEvent_logsReceivedAndSuccess() {
        UUID saleId = UUID.randomUUID();
        SaleEventDto event = new SaleEventDto(
                saleId, 1L, "test@example.com",
                LocalDateTime.now(),
                List.of(new SaleEventItemDto(10L, "Product", 1, new BigDecimal("10.00")))
        );
        SaleResponse fakeResponse = new SaleResponse(99L, 1L, BigDecimal.TEN, null, LocalDateTime.now(), List.of());
        when(saleRepository.existsBySaleEventId(saleId.toString())).thenReturn(false);
        when(saleService.create(any())).thenReturn(fakeResponse);
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        saleEventConsumerService.consume(event);

        List<String> messages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(listAppender.list).anyMatch(e ->
                e.getLevel() == Level.INFO && e.getFormattedMessage().toLowerCase().contains("received"));
        assertThat(listAppender.list).anyMatch(e ->
                e.getLevel() == Level.INFO && e.getFormattedMessage().toLowerCase().contains("success"));
    }

    @Test
    void consume_newEvent_publishesSaleCompletedEventToKafka() {
        UUID saleId = UUID.randomUUID();
        SaleResponse created = new SaleResponse(99L, 1L, BigDecimal.TEN, null, LocalDateTime.now(), List.of());
        when(saleRepository.existsBySaleEventId(saleId.toString())).thenReturn(false);
        when(saleService.create(any())).thenReturn(created);

        saleEventConsumerService.consume(buildEvent(saleId));

        ArgumentCaptor<SaleCompletedEvent> captor = ArgumentCaptor.forClass(SaleCompletedEvent.class);
        verify(saleCompletedPublisher).publishAndWait(captor.capture());
        assertThat(captor.getValue().saleId()).isEqualTo(99L);
        assertThat(captor.getValue().saleEventId()).isEqualTo(saleId.toString());
    }

    @Test
    void consume_duplicateEvent_republishesStoredSaleSoKafkaIsNotMissed() {
        UUID saleId = UUID.randomUUID();
        SaleResponse stored = new SaleResponse(99L, 1L, BigDecimal.TEN, null, LocalDateTime.now(), List.of());
        when(saleRepository.existsBySaleEventId(saleId.toString())).thenReturn(true);
        when(saleService.findBySaleEventId(saleId.toString())).thenReturn(Optional.of(stored));

        saleEventConsumerService.consume(buildEvent(saleId));

        verify(saleService, never()).create(any());
        verify(saleCompletedPublisher).publishAndWait(any());
    }

    @Test
    void consume_kafkaPublishFails_saleIsStillPersistedAndRabbitMessageIsNotFailed() {
        // Kafka must not affect the existing RabbitMQ integration (EP5 constraint).
        UUID saleId = UUID.randomUUID();
        SaleResponse created = new SaleResponse(99L, 1L, BigDecimal.TEN, null, LocalDateTime.now(), List.of());
        when(saleRepository.existsBySaleEventId(saleId.toString())).thenReturn(false);
        when(saleService.create(any())).thenReturn(created);
        doThrow(new KafkaPublishException("broker down", null))
                .when(saleCompletedPublisher).publishAndWait(any());

        assertDoesNotThrow(() -> saleEventConsumerService.consume(buildEvent(saleId)));

        verify(saleService).create(any());
        assertThat(listAppender.list).anyMatch(e ->
                e.getLevel() == Level.ERROR && e.getFormattedMessage().contains("Kafka event was not published"));
    }

    private static SaleEventDto buildEvent(UUID saleId) {
        return new SaleEventDto(
                saleId, 1L, "test@example.com",
                LocalDateTime.now(),
                List.of(new SaleEventItemDto(10L, "Product", 1, new BigDecimal("10.00")))
        );
    }

    @Test
    void consume_duplicateEvent_logsWarn() {
        UUID saleId = UUID.randomUUID();
        SaleEventDto event = new SaleEventDto(
                saleId, 1L, "test@example.com",
                LocalDateTime.now(),
                List.of(new SaleEventItemDto(10L, "Product", 1, new BigDecimal("10.00")))
        );
        when(saleRepository.existsBySaleEventId(saleId.toString())).thenReturn(true);

        saleEventConsumerService.consume(event);

        assertThat(listAppender.list).anyMatch(e ->
                e.getLevel() == Level.WARN
                        && (e.getFormattedMessage().toLowerCase().contains("duplicate")
                                || e.getFormattedMessage().toLowerCase().contains("skipping")));
        assertThat(listAppender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }
}
