package com.nguyengiau.example10.repository;

import com.nguyengiau.example10.cafe.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Long>{
    
}