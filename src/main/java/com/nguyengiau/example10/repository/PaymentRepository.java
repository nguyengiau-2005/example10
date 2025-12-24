package com.nguyengiau.example10.repository;

import com.nguyengiau.example10.cafe.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Nếu cần các truy vấn tuỳ chỉnh có thể thêm ở đây
    Payment findByOrderId(Long orderId);
}
