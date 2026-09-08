package com.payment.model;

public enum SagaStatus {
    STARTED,             // Saga initiated
    PAYMENT_COMPLETED,   // Payment Service debited funds successfully
    LEDGER_COMPLETED,    // Ledger Service recorded accounting entries
    COMPENSATING,        // A step failed; actively rolling back earlier steps
    COMPENSATED,         // All compensating steps finished cleanly
    FAILED,              // Saga failed and compensations are complete
    SUCCESS              // All forward steps completed successfully
}