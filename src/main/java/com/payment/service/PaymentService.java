package com.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.PaymentCreatedEvent;
import com.payment.dto.PaymentRequest;
import com.payment.entity.OutboxEntity;
import com.payment.entity.PaymentEntity;
import com.payment.model.PaymentStatus;
import com.payment.repository.OutboxRepository;
import com.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;
    private final MeterRegistry meterRegistry;

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxRepository outboxRepository,
                          ObjectMapper objectMapper,
                          IdempotencyService idempotencyService,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public PaymentEntity processPayment(PaymentRequest request) {
        //  Step 1: Layer 1 Redis Distributed Lock Check
        String lockKey = "lock:payment:" + request.idempotencyKey();
        boolean locked = idempotencyService.lock(lockKey, 10); // 10s TTL

        if (!locked) {
            throw new IllegalStateException("Concurrent payment request already in progress for key: " + request.idempotencyKey());
        }

        //  Step 2: Layer 2 Check DB for existing completed record
        Optional<PaymentEntity> existingPayment = paymentRepository.findById(request.idempotencyKey());
        if (existingPayment.isPresent()) {
            meterRegistry.counter("payment.idempotent.total").increment();
            return existingPayment.get();
        }

        //  Step 3: Create Payment Record
        PaymentEntity newPayment = new PaymentEntity(
                request.idempotencyKey(),
                request.amount(),
                PaymentStatus.SUCCESS,
                "USER_101",
                LocalDateTime.now()
        );

        PaymentEntity savedPayment;
        try {
            savedPayment = paymentRepository.save(newPayment);
        } catch (DataIntegrityViolationException e) {
            return paymentRepository.findById(request.idempotencyKey())
                    .orElseThrow(() -> e);
        }

        //  Step 4: Create Outbox Event
        saveOutboxEvent(savedPayment);

        meterRegistry.counter("payment.successful.total").increment();

        return savedPayment;
    }

    private void saveOutboxEvent(PaymentEntity payment) {
        try {
            PaymentCreatedEvent eventPayload = new PaymentCreatedEvent(
                    payment.getTransactionId(),
                    payment.getUserId(),
                    payment.getAmount(),
                    payment.getStatus().name(),
                    payment.getCreatedAt()
            );

            String jsonPayload = objectMapper.writeValueAsString(eventPayload);

            OutboxEntity outboxEntry = new OutboxEntity(
                    "PAYMENT",
                    payment.getTransactionId(),
                    "PAYMENT_CREATED",
                    jsonPayload
            );

            outboxRepository.save(outboxEntry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}