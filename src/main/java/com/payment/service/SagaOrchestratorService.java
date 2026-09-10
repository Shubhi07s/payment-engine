package com.payment.service;

import com.payment.entity.SagaInstanceEntity;
import com.payment.model.SagaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SagaOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestratorService.class);

    private final SagaStateService sagaStateService;

    public SagaOrchestratorService(SagaStateService sagaStateService) {
        this.sagaStateService = sagaStateService;
    }

    public void executeSagaWorkflow(String sagaId) {
        try {
            // 1. Advance to PAYMENT_COMPLETED
            sagaStateService.updateStatus(sagaId, SagaStatus.PAYMENT_COMPLETED);

            // 2. Execute Ledger Step
            boolean ledgerSuccess = executeLedger();
            if (!ledgerSuccess) {
                triggerCompensation(sagaId, "Ledger failed");
                return;
            }

            // 3. Complete Saga
            sagaStateService.updateStatus(sagaId, SagaStatus.SUCCESS);

        } catch (Exception e) {
            log.error("Error executing saga {}. Initiating compensation.", sagaId, e);
            triggerCompensation(sagaId, e.getMessage());
        }
    }

    public void triggerCompensation(String sagaId, String reason) {
        log.warn("Compensating saga {}. Reason: {}", sagaId, reason);

        // Transaction 1: Record COMPENSATING state safely
        SagaInstanceEntity saga = sagaStateService.updateStatus(sagaId, SagaStatus.COMPENSATING);

        // Network Call: Attempt Refund
        refundPayment(saga.getTransactionId());

        // Transaction 2: Record COMPENSATED state
        sagaStateService.updateStatus(sagaId, SagaStatus.COMPENSATED);
    }

    private boolean executeLedger() { return false; } // Mock failure
    private void refundPayment(String transactionId) {
        log.info("Refunding payment for transaction: {}", transactionId);
    }
}