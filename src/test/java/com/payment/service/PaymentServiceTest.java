package com.payment.service;

import com.payment.dto.PaymentRequest;
import com.payment.entity.PaymentEntity;
import com.payment.model.PaymentStatus;
import com.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_whenPaymentAlreadyExists_shouldReturnExistingPayment() {
        // 1. Arrange (Given)
        PaymentRequest request = new PaymentRequest("KEY_123", new BigDecimal("100.00"), "USD");
        PaymentEntity existingPayment = new PaymentEntity(
                "KEY_123", new BigDecimal("100.00"), PaymentStatus.SUCCESS, "USER_101", LocalDateTime.now()
        );

        when(paymentRepository.findById("KEY_123")).thenReturn(Optional.of(existingPayment));

        // 2. Act (When)
        PaymentEntity result = paymentService.processPayment(request);

        // 3. Assert (Then)
        assertEquals("KEY_123", result.getTransactionId());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_whenNewPayment_shouldSaveAndReturnPayment() {
        // 1. Arrange (Given)
        PaymentRequest request = new PaymentRequest("KEY_123", new BigDecimal("100.00"), "USD");
        PaymentEntity expectedEntity = new PaymentEntity(
                "KEY_123", new BigDecimal("100.00"), PaymentStatus.SUCCESS, "USER_101", LocalDateTime.now()
        );

        // Mock findById returning empty (no existing transaction)
        when(paymentRepository.findById("KEY_123")).thenReturn(Optional.empty());
        // Mock save returning the newly persisted entity
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(expectedEntity);

        // 2. Act (When)
        PaymentEntity result = paymentService.processPayment(request);

        // 3. Assert (Then)
        assertNotNull(result);
        assertEquals("KEY_123", result.getTransactionId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        // Verify save was called exactly once
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }
}