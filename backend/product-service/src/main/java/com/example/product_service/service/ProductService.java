package com.example.product_service.service;

import com.example.product_service.dto.ProductChangedEvent;
import com.example.product_service.dto.ProductChangedEvent.ChangeType;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.active() == null || request.active()
        );
        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(ProductChangedEvent.of(ChangeType.CREATED, saved));
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        if (request.active() != null) {
            product.setActive(request.active());
        }
        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(ProductChangedEvent.of(ChangeType.UPDATED, saved));
        return ProductResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
        eventPublisher.publishEvent(ProductChangedEvent.of(ChangeType.DELETED, product));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado: " + id));
    }
}
