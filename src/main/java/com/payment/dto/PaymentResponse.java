package com.payment.dto;

import com.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String transactionId,
        BigDecimal amount,
        PaymentStatus status,
        String userId,
        LocalDateTime createdAt
) {}