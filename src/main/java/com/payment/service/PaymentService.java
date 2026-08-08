package com.payment.service;

import com.payment.entity.PaymentEntity;
import com.payment.model.Payment;
import com.payment.model.PaymentStatus;
import com.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public String processPayment(Payment payment) {
        // 1. Sequential check: If record already exists in DB
        Optional<PaymentEntity> existingPayment = paymentRepository.findById(payment.transactionId());
        if (existingPayment.isPresent()) {
            return "Payment already processed for ID: " + payment.transactionId();
        }

        // 2. Prepare new entity
        PaymentEntity newPayment = new PaymentEntity(
                payment.transactionId(),
                payment.amount(),
                PaymentStatus.SUCCESS,
                "USER_101",
                LocalDateTime.now()
        );

        // 3. Concurrent protection: Catch duplicate primary key insertion
        try {
            paymentRepository.save(newPayment);
            return "Payment processed successfully for ID: " + payment.transactionId();
        } catch (DataIntegrityViolationException e) {
            // Thread A saved it a millisecond ago! Thread B safely returns the idempotent result.
            return "Payment already processed for ID: " + payment.transactionId();
        }
    }
}