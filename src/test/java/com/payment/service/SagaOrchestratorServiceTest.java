package com.payment.service;

import com.payment.entity.SagaInstanceEntity;
import com.payment.model.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorServiceTest {

    @Mock
    private SagaStateService sagaStateService;

    private SagaOrchestratorService sagaOrchestratorService;

    @BeforeEach
    void setUp() {
        sagaOrchestratorService = new SagaOrchestratorService(sagaStateService);
    }

    @Test
    void executeSagaWorkflow_shouldTriggerCompensation_whenLedgerFails() {
        // Given
        String sagaId = "SAGA_999";
        String transactionId = "TXN_888";
        SagaInstanceEntity mockSaga = new SagaInstanceEntity(sagaId, transactionId, SagaStatus.STARTED);

        when(sagaStateService.updateStatus(eq(sagaId), any(SagaStatus.class)))
                .thenReturn(mockSaga);

        // When
        sagaOrchestratorService.executeSagaWorkflow(sagaId);

        // Then  - Enforce exact order of state transitions
        InOrder inOrder = inOrder(sagaStateService);

        // 1. First transitions to PAYMENT_COMPLETED
        inOrder.verify(sagaStateService).updateStatus(sagaId, SagaStatus.PAYMENT_COMPLETED);

        // 2. Then transitions to COMPENSATING when Ledger fails
        inOrder.verify(sagaStateService).updateStatus(sagaId, SagaStatus.COMPENSATING);

        // 3. Finally transitions to COMPENSATED after refunding
        inOrder.verify(sagaStateService).updateStatus(sagaId, SagaStatus.COMPENSATED);
    }
}