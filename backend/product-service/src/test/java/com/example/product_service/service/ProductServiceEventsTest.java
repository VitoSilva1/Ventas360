package com.example.product_service.service;

import com.example.product_service.dto.ProductChangedEvent;
import com.example.product_service.dto.ProductChangedEvent.ChangeType;
import com.example.product_service.dto.ProductRequest;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceEventsTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    @Test
    void create_emitsCreatedEventWithSavedState() {
        when(productRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), 5L));

        productService.create(new ProductRequest("Mouse", "USB", new BigDecimal("9990"), 10, null));

        ProductChangedEvent event = captureEvent();
        assertThat(event.type()).isEqualTo(ChangeType.CREATED);
        assertThat(event.productId()).isEqualTo(5L);
        assertThat(event.name()).isEqualTo("Mouse");
        assertThat(event.stock()).isEqualTo(10);
        assertThat(event.active()).isTrue();
    }

    @Test
    void update_emitsUpdatedEvent() {
        Product existing = withId(new Product("Mouse", "USB", new BigDecimal("9990"), 10, true), 5L);
        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        productService.update(5L, new ProductRequest("Mouse Pro", "USB", new BigDecimal("12990"), 4, false));

        ProductChangedEvent event = captureEvent();
        assertThat(event.type()).isEqualTo(ChangeType.UPDATED);
        assertThat(event.name()).isEqualTo("Mouse Pro");
        assertThat(event.stock()).isEqualTo(4);
        assertThat(event.active()).isFalse();
    }

    @Test
    void delete_emitsDeletedEventWithLastKnownState() {
        Product existing = withId(new Product("Mouse", "USB", new BigDecimal("9990"), 10, true), 5L);
        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));

        productService.delete(5L);

        ProductChangedEvent event = captureEvent();
        assertThat(event.type()).isEqualTo(ChangeType.DELETED);
        assertThat(event.productId()).isEqualTo(5L);
        assertThat(event.name()).isEqualTo("Mouse");
    }

    @Test
    void update_missingProduct_emitsNothing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L,
                new ProductRequest("X", null, BigDecimal.ONE, 1, true)))
                .isInstanceOf(ResponseStatusException.class);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private ProductChangedEvent captureEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ProductChangedEvent.class);
        return (ProductChangedEvent) captor.getValue();
    }

    private static Product withId(Product product, Long id) {
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
