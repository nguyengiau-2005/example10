package com.nguyengiau.example10.repository;

import com.nguyengiau.example10.cafe.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    Boolean existsByUsername(String username);
}
