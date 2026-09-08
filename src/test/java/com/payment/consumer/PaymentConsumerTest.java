package com.payment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.PaymentCreatedEvent;
import com.payment.entity.PaymentEntity;
import com.payment.model.PaymentStatus;
import com.payment.repository.PaymentRepository;
import com.payment.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentConsumerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyService idempotencyService;

    private ObjectMapper objectMapper;
    private PaymentConsumer paymentConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        paymentConsumer = new PaymentConsumer(paymentRepository, idempotencyService, objectMapper);
    }

    @Test
    void consume_shouldProcessPaymentAndUpdateStatusToSuccess_whenPending() throws Exception {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "KEY_123", "USER_1", new BigDecimal("100.00"), "PENDING", LocalDateTime.now()
        );
        String jsonMessage = objectMapper.writeValueAsString(event);
        PaymentEntity entity = new PaymentEntity(
                "KEY_123", new BigDecimal("100.00"), PaymentStatus.PENDING, "USER_1", LocalDateTime.now()
        );

        when(idempotencyService.lock(eq("lock:consumer:KEY_123"), eq(30L))).thenReturn(true);
        when(paymentRepository.findById("KEY_123")).thenReturn(Optional.of(entity));

        paymentConsumer.consume(jsonMessage);

        assertEquals(PaymentStatus.SUCCESS, entity.getStatus());
        verify(paymentRepository).save(entity);
        verify(idempotencyService).unlock("lock:consumer:KEY_123");
    }

    @Test
    void consume_shouldSkipProcessing_whenStatusIsAlreadySuccess() throws Exception {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "KEY_123", "USER_1", new BigDecimal("100.00"), "SUCCESS", LocalDateTime.now()
        );
        String jsonMessage = objectMapper.writeValueAsString(event);
        PaymentEntity entity = new PaymentEntity(
                "KEY_123", new BigDecimal("100.00"), PaymentStatus.SUCCESS, "USER_1", LocalDateTime.now()
        );

        when(idempotencyService.lock(eq("lock:consumer:KEY_123"), eq(30L))).thenReturn(true);
        when(paymentRepository.findById("KEY_123")).thenReturn(Optional.of(entity));

        paymentConsumer.consume(jsonMessage);

        verify(paymentRepository, never()).save(any());
        verify(idempotencyService).unlock("lock:consumer:KEY_123");
    }

    @Test
    void consume_shouldThrowException_whenLockAcquisitionFails() throws Exception {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "KEY_123", "USER_1", new BigDecimal("100.00"), "PENDING", LocalDateTime.now()
        );
        String jsonMessage = objectMapper.writeValueAsString(event);

        when(idempotencyService.lock(eq("lock:consumer:KEY_123"), eq(30L))).thenReturn(false);

        assertThrows(TransientDataAccessException.class, () -> paymentConsumer.consume(jsonMessage));
        verify(paymentRepository, never()).findById(any());
    }
}