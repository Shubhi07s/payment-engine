package com.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.PaymentRequest;
import com.payment.entity.PaymentEntity;
import com.payment.exception.GlobalExceptionHandler;
import com.payment.model.PaymentStatus;
import com.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@WebMvcTest(PaymentEngineController.class)
@Import(GlobalExceptionHandler.class)
class PaymentEngineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    // 1. Existing Validation Test (400 Bad Request)
    @Test
    void processPayment_whenAmountIsNegative_shouldReturn400BadRequest() throws Exception {
        String invalidJson = """
            {
                "idempotencyKey": "KEY_123",
                "amount": -50.00,
                "currency": "USD"
            }
            """;

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").value("Amount must be greater than zero"));
    }

    // 2. Success Path Test (200 OK)
    @Test
    void processPayment_shouldReturn200AndPaymentResponse_whenSuccessful() throws Exception {
        PaymentRequest request = new PaymentRequest("KEY_123", new BigDecimal("100.00"), "USD");
        PaymentEntity mockEntity = new PaymentEntity(
                "KEY_123", new BigDecimal("100.00"), PaymentStatus.SUCCESS, "USER_101", LocalDateTime.now()
        );

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(mockEntity);

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("KEY_123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // 3. Concurrent Lock Failure Test (409 Conflict)
    @Test
    void processPayment_shouldReturn409Conflict_whenConcurrentLockFails() throws Exception {
        PaymentRequest request = new PaymentRequest("KEY_123", new BigDecimal("100.00"), "USD");

        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenThrow(new IllegalStateException("Concurrent payment request in progress for key: KEY_123"));

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}