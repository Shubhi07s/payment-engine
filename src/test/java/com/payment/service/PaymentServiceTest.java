package com.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payment.dto.PaymentRequest;
import com.payment.entity.OutboxEntity;
import com.payment.entity.PaymentEntity;
import com.payment.model.PaymentStatus;
import com.payment.repository.OutboxRepository;
import com.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private IdempotencyService idempotencyService;

    // Use a real ObjectMapper to handle serialization cleanly
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                outboxRepository,
                objectMapper,
                idempotencyService
        );
    }

    @Test
    void processPayment_whenLockAcquiredAndNewPayment_shouldSaveAndReturnPayment() {
        // 1. Arrange (Given)
        PaymentRequest request = new PaymentRequest("KEY_123", new BigDecimal("100.00"), "USD");
        PaymentEntity expectedEntity = new PaymentEntity(
                "KEY_123", new BigDecimal("100.00"), PaymentStatus.SUCCESS, "USER_101", LocalDateTime.now()
        );

        // Lock acquisition succeeds
        when(idempotencyService.lock(anyString(), anyLong())).thenReturn(true);
        when(paymentRepository.findById("KEY_123")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(expectedEntity);

        // 2. Act (When)
        PaymentEntity result = paymentService.processPayment(request);

        // 3. Assert (Then)
        assertNotNull(result);
        assertEquals("KEY_123", result.getTransactionId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        // Verify lock, payment save, and outbox save occurred 💾
        verify(idempotencyService, times(1)).lock("lock:payment:KEY_123", 10);
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
        verify(outboxRepository, times(1)).save(any(OutboxEntity.class));
    }

    @Test
    void processPayment_whenLockFails_shouldThrowExceptionAndNotTouchDatabase() {
        // 1. Arrange (Given)
        PaymentRequest request = new PaymentRequest("KEY_123", new BigDecimal("100.00"), "USD");

        // Lock acquisition fails (concurrent request in progress)
        when(idempotencyService.lock(anyString(), anyLong())).thenReturn(false);

        // 2. Act & 3. Assert
        assertThrows(IllegalStateException.class, () -> paymentService.processPayment(request));

        // Verify database was NEVER touched 🛡️
        verify(paymentRepository, never()).findById(anyString());
        verify(paymentRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }
}