package com.nguyengiau.example10.controllers;

import com.nguyengiau.example10.cafe.entity.Payment;
import com.nguyengiau.example10.cafe.entity.enums.PaymentMethod;
import com.nguyengiau.example10.cafe.entity.enums.PaymentStatus;
import com.nguyengiau.example10.security.services.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
public class PaymentController {

    private final OrderService orderService;

    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    // xem tất cả payment
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = orderService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    // Tạo payment mới liên kết với đơn hàng
    @PostMapping("/create")
    public ResponseEntity<Payment> createPayment(
            @RequestParam Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod method) {

        Payment payment = orderService.createPayment(orderId, method);
        return ResponseEntity.ok(payment);
    }

    // Cập nhật trạng thái payment
    @PutMapping("/{paymentId}/status")
    public ResponseEntity<Payment> updatePaymentStatus(
            @PathVariable Long paymentId,
            @RequestParam PaymentStatus status) {

        Payment payment = orderService.updatePaymentStatus(paymentId, status);
        return ResponseEntity.ok(payment);
    }
}
