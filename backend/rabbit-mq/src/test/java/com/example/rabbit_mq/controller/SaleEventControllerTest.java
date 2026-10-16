package com.example.rabbit_mq.controller;

import com.example.rabbit_mq.service.SaleEventProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SaleEventController.class)
class SaleEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SaleEventProducerService saleEventProducerService;

    private static final String VALID_BODY = """
        {
          "saleId": "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c",
          "customerId": null,
          "customerEmail": "buyer@example.com",
          "saleDate": "2026-01-01T10:00:00.000Z",
          "items": [
            { "productId": 10, "productName": "Keyboard", "quantity": 2, "unitPrice": 75.00 }
          ]
        }
        """;

    @Test
    void publishSaleEvent_valid_returns202() throws Exception {
        when(saleEventProducerService.publish(any())).thenReturn("test-uuid");
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.eventId").value("test-uuid"));
    }

    @Test
    void publishSaleEvent_nullSaleId_returns400() throws Exception {
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("\"saleId\": \"3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c\",", "")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publishSaleEvent_nullSaleDate_returns400() throws Exception {
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("\"saleDate\": \"2026-01-01T10:00:00.000Z\",", "")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publishSaleEvent_emptyItems_returns400() throws Exception {
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replaceAll("(?s)\"items\": \\[.*?\\]", "\"items\": []")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publishSaleEvent_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("\"quantity\": 2", "\"quantity\": 0")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publishSaleEvent_responseContainsExchange() throws Exception {
        when(saleEventProducerService.publish(any())).thenReturn("test-uuid");
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.exchange").exists());
    }
}
