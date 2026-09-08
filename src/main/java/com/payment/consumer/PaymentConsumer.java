package com.payment.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.PaymentCreatedEvent;
import com.payment.entity.PaymentEntity;
import com.payment.model.PaymentStatus;
import com.payment.repository.PaymentRepository;
import com.payment.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public PaymentConsumer(PaymentRepository paymentRepository,
                           IdempotencyService idempotencyService,
                           ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            exclude = {JsonProcessingException.class, IllegalArgumentException.class}
    )
    @KafkaListener(topics = "payment-events", groupId = "payment-engine-group")
    public void consume(String message) throws JsonProcessingException {
        log.info("Received Kafka payment event: {}", message);

        // 1. Safe JSON Deserialization
        PaymentCreatedEvent event = objectMapper.readValue(message, PaymentCreatedEvent.class);
        String transactionId = event.transactionId();

        // 2. Redis Distributed Lock Guard
        String lockKey = "lock:consumer:" + transactionId;
        boolean locked = idempotencyService.lock(lockKey, 30L);

        if (!locked) {
            log.warn("Concurrent consumer processing active for key: {}. Skipping.", transactionId);
            // Throwing an exception forces @RetryableTopic to back off and retry later
            throw new TransientDataAccessException("Could not acquire consumer processing lock for key: " + transactionId) {};        }

        try {
            // 3. PostgreSQL State Verification
            Optional<PaymentEntity> entityOpt = paymentRepository.findById(transactionId);
            if (entityOpt.isEmpty()) {
                log.error("Payment record not found for transactionId: {}", transactionId);
                throw new IllegalArgumentException("Invalid transaction ID: " + transactionId);
            }

            PaymentEntity entity = entityOpt.get();
            if (entity.getStatus() != PaymentStatus.PENDING) {
                log.info("Payment {} already processed with status {}. Skipping.", transactionId, entity.getStatus());
                return;
            }

            // 4. Simulate Processing & Update State
            log.info("Processing payment for transactionId: {}", transactionId);
            entity.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(entity);
            log.info("Successfully processed and updated status to SUCCESS for key: {}", transactionId);

        } finally {
            // 5. Always Release Redis Lock
            idempotencyService.unlock(lockKey);
        }
    }
}