package com.payment.repository;

import com.payment.entity.OutboxEntity;
import com.payment.model.OutboxStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    // 🔒 Locks fetched rows and skips any rows locked by other instances
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")}) // -2 tells Postgres to SKIP LOCKED
    List<OutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}