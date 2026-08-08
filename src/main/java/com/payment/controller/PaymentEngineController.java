package com.payment.controller;

import com.payment.dto.PaymentRequest;
import com.payment.entity.PaymentEntity;
import com.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentEngineController {

    private final PaymentService paymentService;

    public PaymentEngineController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentEntity> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentEntity response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
}