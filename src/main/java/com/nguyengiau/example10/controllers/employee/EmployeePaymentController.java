package com.nguyengiau.example10.controllers.employee;

import com.nguyengiau.example10.cafe.entity.Payment;
import com.nguyengiau.example10.cafe.entity.enums.PaymentMethod;
import com.nguyengiau.example10.cafe.entity.enums.PaymentStatus;
import com.nguyengiau.example10.security.services.OrderService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/employee")
public class EmployeePaymentController {

    private final OrderService orderService;

    public EmployeePaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

@PostMapping("/payments/order") // ví dụ đổi tên endpoint
public ResponseEntity<Payment> createPayment(
        @RequestParam Long orderId,
        @RequestParam PaymentMethod method) {
    Payment payment = orderService.createPayment(orderId, method);
    return ResponseEntity.ok(payment);
}

@PutMapping("/payments/order/{id}/status")
public ResponseEntity<Payment> updatePaymentStatus(
        @PathVariable Long id,
        @RequestParam PaymentStatus status) {
    Payment payment = orderService.updatePaymentStatus(id, status);
    return ResponseEntity.ok(payment);
}
}
