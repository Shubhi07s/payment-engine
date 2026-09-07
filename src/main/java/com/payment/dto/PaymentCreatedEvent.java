package com.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCreatedEvent(
        String transactionId,
        String userId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {}