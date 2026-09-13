package com.payment.service;

import com.payment.entity.PaymentEntity;
import com.payment.entity.SagaInstanceEntity;
import com.payment.model.PaymentStatus;
import com.payment.model.SagaStatus;
import com.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SagaOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestratorService.class);

    private final SagaStateService sagaStateService;
    private final PaymentRepository paymentRepository;

    public SagaOrchestratorService(SagaStateService sagaStateService, PaymentRepository paymentRepository) {
        this.sagaStateService = sagaStateService;
        this.paymentRepository = paymentRepository;
    }

    public void executeSagaWorkflow(String transactionId) {
        String sagaId = transactionId;
        try {
            // Step 0: State Genesis
            sagaStateService.createSaga(sagaId, transactionId);

            // Step 1: Advance state to PAYMENT_COMPLETED
            sagaStateService.updateStatus(sagaId, SagaStatus.PAYMENT_COMPLETED);

            // Step 2: Execute Ledger Step (Mock downstream integration)
            boolean ledgerSuccess = executeLedger();
            if (!ledgerSuccess) {
                triggerCompensation(sagaId, "Ledger processing failed");
                return;
            }

            // Step 3: Saga Success -> Update Saga State + Payment Aggregate
            sagaStateService.updateStatus(sagaId, SagaStatus.SUCCESS);
            updatePaymentStatus(transactionId, PaymentStatus.SUCCESS);

        } catch (Exception e) {
            log.error("Error executing saga {}. Initiating compensation.", sagaId, e);
            triggerCompensation(sagaId, e.getMessage());
        }
    }

    public void triggerCompensation(String sagaId, String reason) {
        log.warn("Compensating saga {}. Reason: {}", sagaId, reason);

        // 1. Record COMPENSATING state safely in its own isolated transaction
        SagaInstanceEntity saga = sagaStateService.updateStatus(sagaId, SagaStatus.COMPENSATING);

        // 2. Perform Compensating Action (Refund / Reversal)
        refundPayment(saga.getTransactionId());

        // 3. Mark Saga COMPENSATED + Mark Domain Payment FAILED
        sagaStateService.updateStatus(sagaId, SagaStatus.COMPENSATED);
        updatePaymentStatus(saga.getTransactionId(), PaymentStatus.FAILED);
    }

    private void updatePaymentStatus(String transactionId, PaymentStatus status) {
        paymentRepository.findById(transactionId).ifPresent(payment -> {
            payment.setStatus(status);
            paymentRepository.save(payment);
            log.info("Domain aggregate 'payments' updated to status: {} for transaction: {}", status, transactionId);
        });
    }

    private boolean executeLedger() {
        return false; // Simulated downstream failure for rollback testing
    }

    private void refundPayment(String transactionId) {
        log.info("Refunding payment for transaction: {}", transactionId);
    }
}