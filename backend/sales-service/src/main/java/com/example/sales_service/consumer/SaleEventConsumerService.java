package com.example.sales_service.consumer;

import com.example.sales_service.dto.SaleEventDto;
import com.example.sales_service.dto.SaleItemRequest;
import com.example.sales_service.dto.SaleRequest;
import com.example.sales_service.repository.SaleRepository;
import com.example.sales_service.service.SaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class SaleEventConsumerService {

    private static final Logger log = LoggerFactory.getLogger(SaleEventConsumerService.class);

    private final SaleService saleService;
    private final SaleRepository saleRepository;

    public SaleEventConsumerService(SaleService saleService, SaleRepository saleRepository) {
        this.saleService = saleService;
        this.saleRepository = saleRepository;
    }

    @RabbitListener(queues = "${rabbitmq.ventas360.queues.sales}")
    public void consume(SaleEventDto event) {
        String eventId = event.saleId().toString();
        log.info("Sale event received saleId={}", eventId);

        if (saleRepository.existsBySaleEventId(eventId)) {
            log.warn("Duplicate event saleId={}, skipping", eventId);
            return;
        }

        try {
            var items = event.items().stream()
                    .map(i -> new SaleItemRequest(i.productId(), i.quantity(), i.unitPrice()))
                    .toList();

            var request = new SaleRequest(event.customerId(), items);
            var response = saleService.create(request);

            saleRepository.findById(response.id()).ifPresent(sale -> {
                sale.setSaleEventId(eventId);
                saleRepository.save(sale);
            });

            log.info("Sale event processed successfully saleId={} dbId={}", eventId, response.id());
        } catch (Exception e) {
            log.error("Failed to process sale event saleId={}: {}", eventId, e.getMessage(), e);
            throw e;
        }
    }
}
