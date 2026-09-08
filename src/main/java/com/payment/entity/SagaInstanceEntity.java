package com.payment.entity;

import com.payment.model.SagaStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_instances")
public class SagaInstanceEntity {

    @Id
    private String sagaId;

    @Column(nullable = false)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    public SagaInstanceEntity() {}

    public SagaInstanceEntity(String sagaId, String transactionId, SagaStatus status) {
        this.sagaId = sagaId;
        this.transactionId = transactionId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getSagaId() { return sagaId; }
    public String getTransactionId() { return transactionId; }
    public SagaStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setStatus(SagaStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now(); // Automatically updates timestamp on state change
    }
}