package com.payment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.payment.model.OutboxStatus;

@Entity
@Table(name = "outbox", indexes = {
        //  Index to speed up pending event lookups for our background worker
        @Index(name = "idx_outbox_status_created", columnList = "status, createdAt")
})
public class OutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // e.g., "PAYMENT"

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;     // e.g., "PAYMENT_CREATED"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;       // JSON string containing payment details

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;  // PENDING, PROCESSED, FAILED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public OutboxEntity() {}

    public OutboxEntity(String aggregateType, String aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Getters for reading event details
    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setter for updating status (PENDING -> PROCESSED)
    public void setStatus(OutboxStatus status) {
        this.status = status;
    }
}