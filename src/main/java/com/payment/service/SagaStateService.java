package com.payment.service;

import com.payment.entity.SagaInstanceEntity;
import com.payment.model.SagaStatus;
import com.payment.repository.SagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SagaStateService {

    private final SagaRepository sagaRepository;

    public SagaStateService(SagaRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SagaInstanceEntity updateStatus(String sagaId, SagaStatus newStatus) {
        SagaInstanceEntity saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga not found: " + sagaId));

        saga.setStatus(newStatus);
        return sagaRepository.save(saga); // Commits immediately in a fresh transaction 💾
    }
}