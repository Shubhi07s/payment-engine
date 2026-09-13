package com.payment.service;

import com.payment.entity.SagaInstanceEntity;
import com.payment.model.SagaStatus;
import com.payment.repository.SagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SagaStateService {

    private final SagaRepository sagaRepository;

    public SagaStateService(SagaRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SagaInstanceEntity createSaga(String sagaId, String transactionId) {
        SagaInstanceEntity saga = new SagaInstanceEntity();
        saga.setSagaId(sagaId);
        saga.setTransactionId(transactionId);
        saga.setStatus(SagaStatus.STARTED);
        saga.setCreatedAt(LocalDateTime.now());
        saga.setUpdatedAt(LocalDateTime.now());
        return sagaRepository.save(saga);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SagaInstanceEntity updateStatus(String sagaId, SagaStatus status) {
        SagaInstanceEntity saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga not found: " + sagaId));

        saga.setStatus(status);
        return sagaRepository.save(saga);
    }
}