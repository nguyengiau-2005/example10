package com.nguyengiau.example10.repository;

import com.nguyengiau.example10.cafe.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {
}
