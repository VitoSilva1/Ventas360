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
          "saleId": 1,
          "customerId": 2,
          "total": "150.00",
          "status": "COMPLETED",
          "createdAt": "2026-01-01T10:00:00",
          "items": [
            { "productId": 10, "quantity": 2, "unitPrice": "75.00", "subtotal": "150.00" }
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
                .content(VALID_BODY.replace("\"saleId\": 1,", "")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publishSaleEvent_nullTotal_returns400() throws Exception {
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("\"total\": \"150.00\",", "")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publishSaleEvent_emptyItems_returns400() throws Exception {
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("\"items\": [\n    { \"productId\": 10, \"quantity\": 2, \"unitPrice\": \"75.00\", \"subtotal\": \"150.00\" }\n  ]", "\"items\": []")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publishSaleEvent_nullStatus_returns400() throws Exception {
        mockMvc.perform(post("/sales/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("\"status\": \"COMPLETED\",", "")))
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
