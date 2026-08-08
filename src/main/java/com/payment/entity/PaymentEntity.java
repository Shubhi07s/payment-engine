package com.payment.entity;

import com.payment.model.PaymentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private String transactionId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Default constructor required by JPA
    public PaymentEntity() {}

    public PaymentEntity(String transactionId, long amount, PaymentStatus status, String userId, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.userId = userId;
        this.createdAt = createdAt;
    }
}