package com.payment.service;

import com.payment.dto.PaymentRequest;
import com.payment.entity.PaymentEntity;
import com.payment.model.PaymentStatus;
import com.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentEntity processPayment(PaymentRequest request) {
        // 1. Look up existing transaction in PostgreSQL
        Optional<PaymentEntity> existingPayment = paymentRepository.findById(request.idempotencyKey());
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        // 2. Map DTO to new Entity
        PaymentEntity newPayment = new PaymentEntity(
                request.idempotencyKey(),
                request.amount(),
                PaymentStatus.SUCCESS,
                "USER_101",
                LocalDateTime.now()
        );

        // 3. Persist with race-condition fallback
        try {
            return paymentRepository.save(newPayment);
        } catch (DataIntegrityViolationException e) {
            return paymentRepository.findById(request.idempotencyKey())
                    .orElseThrow(() -> e);
        }
    }
}