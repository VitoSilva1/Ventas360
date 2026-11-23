package com.example.product_service.service;

import com.example.product_service.dto.ProductChangedEvent;
import com.example.product_service.dto.SaleCompletedEvent;
import com.example.product_service.messaging.kafka.ProductEventsKafkaRelay;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProcessedSaleRepository;
import com.example.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RecordApplicationEvents
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProcessedSaleRepository processedSaleRepository;

    @Autowired
    private ApplicationEvents events;

    // No broker here: Kafka publishing is covered by ProductEventsKafkaRelayTest.
    @MockitoBean
    private ProductEventsKafkaRelay relay;

    private Long mouseId;
    private Long cableId;

    @BeforeEach
    void seed() {
        processedSaleRepository.deleteAll();
        productRepository.deleteAll();
        mouseId = productRepository.save(new Product("Mouse", null, new BigDecimal("9990"), 10, true)).getId();
        cableId = productRepository.save(new Product("Cable", null, new BigDecimal("1990"), 1, true)).getId();
    }

    @Test
    void applySale_decrementsStockOfEveryItem() {
        assertThat(inventoryService.applySale(sale(1L, item(mouseId, 3), item(cableId, 1)))).isTrue();

        assertThat(stockOf(mouseId)).isEqualTo(7);
        assertThat(stockOf(cableId)).isZero();
    }

    @Test
    void applySale_sameSaleTwice_decrementsOnlyOnce() {
        inventoryService.applySale(sale(2L, item(mouseId, 2)));

        assertThat(inventoryService.applySale(sale(2L, item(mouseId, 2)))).isFalse();
        assertThat(stockOf(mouseId)).isEqualTo(8);
    }

    @Test
    void applySale_moreUnitsThanStock_clampsAtZero() {
        inventoryService.applySale(sale(3L, item(cableId, 5)));

        assertThat(stockOf(cableId)).isZero();
    }

    @Test
    void applySale_unknownProduct_isSkippedAndOtherItemsStillApply() {
        inventoryService.applySale(sale(4L, item(999_999L, 1), item(mouseId, 1)));

        assertThat(stockOf(mouseId)).isEqualTo(9);
    }

    @Test
    void applySale_emitsUpdatedProductEventWithNewStock() {
        inventoryService.applySale(sale(5L, item(mouseId, 4)));

        assertThat(events.stream(ProductChangedEvent.class))
                .anySatisfy(e -> {
                    assertThat(e.type()).isEqualTo(ProductChangedEvent.ChangeType.UPDATED);
                    assertThat(e.productId()).isEqualTo(mouseId);
                    assertThat(e.stock()).isEqualTo(6);
                });
    }

    @Test
    void applySale_sameSaleDeliveredConcurrently_decrementsOnlyOnce() throws Exception {
        // Regression: overlapping redelivery (e.g. during a rebalance) of the same sale.
        Long productId = productRepository.save(new Product("Tablet", null, new BigDecimal("1"), 1000, true)).getId();
        int threads = 6;
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        try {
            List<java.util.concurrent.Future<?>> calls = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                calls.add(pool.submit(() -> {
                    start.await();
                    try {
                        return inventoryService.applySale(sale(777L, item(productId, 5)));
                    } catch (RuntimeException duplicateKey) {
                        // What the Kafka error handler does: retry; the retry must see the sale as applied.
                        return inventoryService.applySale(sale(777L, item(productId, 5)));
                    }
                }));
            }
            start.countDown();
            for (var call : calls) {
                call.get(30, java.util.concurrent.TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(stockOf(productId)).isEqualTo(995);
        assertThat(processedSaleRepository.existsById(777L)).isTrue();
    }

    @Test
    void validation_rejectsNullItemInsideList() {
        var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        var sale = new SaleCompletedEvent(8L, java.util.Arrays.asList(item(mouseId, 1), null));

        assertThat(validator.validate(sale)).isNotEmpty();
    }

    private int stockOf(Long id) {
        return productRepository.findById(id).orElseThrow().getStock();
    }

    private static SaleCompletedEvent sale(Long saleId, SaleCompletedEvent.Item... items) {
        return new SaleCompletedEvent(saleId, List.of(items));
    }

    private static SaleCompletedEvent.Item item(Long productId, int quantity) {
        return new SaleCompletedEvent.Item(productId, quantity);
    }
}
