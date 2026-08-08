package com.payment.controller;

import com.payment.model.Payment;
import com.payment.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentEngineController {

    private final PaymentService paymentService;

    // Spring automatically supplies the PaymentService Bean here!
    public PaymentEngineController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public String processPayment(@RequestBody Payment payment) {
        return paymentService.processPayment(payment);
    }
}