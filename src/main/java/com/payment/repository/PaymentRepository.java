package com.payment.repository;

import com.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    // Standard CRUD methods like findById(), save(), and existsById()
    // are automatically provided by JpaRepository!
}