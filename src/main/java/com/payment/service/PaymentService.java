package com.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.PaymentCreatedEvent;
import com.payment.dto.PaymentRequest;
import com.payment.entity.OutboxEntity;
import com.payment.entity.PaymentEntity;
import com.payment.model.OutboxStatus;
import com.payment.model.PaymentStatus;
import com.payment.repository.OutboxRepository;
import com.payment.repository.PaymentRepository;
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

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxRepository outboxRepository,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentEntity processPayment(PaymentRequest request) {
        // 1. Double-checked Idempotency Check
        Optional<PaymentEntity> existingPayment = paymentRepository.findById(request.idempotencyKey());
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        // 2. Create Payment Record
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

        // 3. Create Outbox Event (Atomic write inside same @Transactional block!)
        saveOutboxEvent(savedPayment);

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