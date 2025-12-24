package com.nguyengiau.example10.security.services;

import com.nguyengiau.example10.cafe.entity.Order;
import com.nguyengiau.example10.cafe.entity.Payment;
import com.nguyengiau.example10.cafe.entity.enums.PaymentMethod;
import com.nguyengiau.example10.cafe.entity.enums.PaymentStatus;
import com.nguyengiau.example10.repository.OrderRepository;
import com.nguyengiau.example10.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    // Lấy tất cả payment
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    // Tạo payment mới
    @Transactional
    public Payment createPayment(Long orderId, BigDecimal amount, PaymentMethod method) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    // Cập nhật trạng thái payment
    @Transactional
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(status);

        return paymentRepository.save(payment);
    }

    // Tìm payment theo orderId
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
