package com.payment.repository;

import com.payment.entity.SagaInstanceEntity;
import com.payment.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SagaRepository extends JpaRepository<SagaInstanceEntity, String> {

    // Find the active saga instance associated with a specific payment transaction
    Optional<SagaInstanceEntity> findByTransactionId(String transactionId);

    // Find sagas stuck in an incomplete state for longer than a given threshold
    @Query("SELECT s FROM SagaInstanceEntity s WHERE s.status IN :statuses AND s.updatedAt < :thresholdTime")
    List<SagaInstanceEntity> findStuckSagas(
            @Param("statuses") List<SagaStatus> statuses,
            @Param("thresholdTime") LocalDateTime thresholdTime
    );
}