package com.example.sales_service.service;

import com.example.sales_service.dto.SaleItemRequest;
import com.example.sales_service.dto.SaleRequest;
import com.example.sales_service.dto.SaleResponse;
import com.example.sales_service.model.Sale;
import com.example.sales_service.model.SaleItem;
import com.example.sales_service.repository.SaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;

    public SaleService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> findAll() {
        return saleRepository.findAll().stream().map(SaleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        return SaleResponse.from(getSale(id));
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        Sale sale = new Sale(request.customerId());
        for (SaleItemRequest item : request.items()) {
            sale.addItem(new SaleItem(item.productId(), item.quantity(), item.unitPrice()));
        }
        return SaleResponse.from(saleRepository.save(sale));
    }

    private Sale getSale(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venta no encontrada: " + id));
    }
}
