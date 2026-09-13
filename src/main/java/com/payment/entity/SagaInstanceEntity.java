package com.payment.entity;

import com.payment.model.SagaStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_instances")
public class SagaInstanceEntity {

    @Id
    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    // Getters
    public String getSagaId() { return sagaId; }
    public String getTransactionId() { return transactionId; }
    public SagaStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public void setStatus(SagaStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}