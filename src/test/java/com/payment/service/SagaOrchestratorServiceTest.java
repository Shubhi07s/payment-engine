package com.payment.service;

import com.payment.entity.PaymentEntity;
import com.payment.entity.SagaInstanceEntity;
import com.payment.model.PaymentStatus;
import com.payment.model.SagaStatus;
import com.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorServiceTest {

    @Mock
    private SagaStateService sagaStateService;

    @Mock
    private PaymentRepository paymentRepository;

    private SagaOrchestratorService sagaOrchestratorService;

    @BeforeEach
    void setUp() {
        // Inject both SagaStateService and PaymentRepository mocks
        sagaOrchestratorService = new SagaOrchestratorService(sagaStateService, paymentRepository);
    }

    @Test
    void executeSagaWorkflow_shouldTriggerCompensationAndMarkPaymentFailed_whenLedgerFails() {
        // Given
        String transactionId = "TXN_888";
        String sagaId = transactionId;

        SagaInstanceEntity mockSaga = new SagaInstanceEntity(sagaId, transactionId, SagaStatus.STARTED);
        PaymentEntity mockPayment = new PaymentEntity(
                transactionId, new BigDecimal("100.00"), PaymentStatus.PENDING, "USER_101", LocalDateTime.now()
        );

        // Stub state service to return our mock saga when transitioning to COMPENSATING
        when(sagaStateService.updateStatus(eq(sagaId), eq(SagaStatus.COMPENSATING)))
                .thenReturn(mockSaga);

        // Stub payment repository to return our pending payment entity
        when(paymentRepository.findById(transactionId))
                .thenReturn(Optional.of(mockPayment));

        // When
        sagaOrchestratorService.executeSagaWorkflow(transactionId);

        // Then - Enforce exact chronological order of execution across services
        InOrder inOrder = inOrder(sagaStateService, paymentRepository);

        // Step 0: State Genesis
        inOrder.verify(sagaStateService).createSaga(sagaId, transactionId);

        // Step 1: Advance to PAYMENT_COMPLETED
        inOrder.verify(sagaStateService).updateStatus(sagaId, SagaStatus.PAYMENT_COMPLETED);

        // Step 2: Transition to COMPENSATING when Mock Ledger fails
        inOrder.verify(sagaStateService).updateStatus(sagaId, SagaStatus.COMPENSATING);

        // Step 3: Transition to COMPENSATED after refunding
        inOrder.verify(sagaStateService).updateStatus(sagaId, SagaStatus.COMPENSATED);

        // Step 4: Final domain aggregate update to FAILED
        inOrder.verify(paymentRepository).save(mockPayment);
    }
}