package com.nguyengiau.example10.security.services;

import com.nguyengiau.example10.cafe.entity.Promotion;
import com.nguyengiau.example10.repository.PromotionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

public List<Promotion> getAll() {
    // Lấy tất cả khuyến mãi còn hiệu lực
    return promotionRepository.findAll(); // hoặc filter active
}

    public Promotion getById(Long id) {
        return promotionRepository.findById(id).orElse(null);
    }

    public Promotion create(Promotion promotion) {
        promotion.setCreatedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());
        return promotionRepository.save(promotion);
    }

    public Promotion update(Long id, Promotion promotion) {
        return promotionRepository.findById(id).map(existing -> {
            existing.setName(promotion.getName());
            existing.setDiscountPercentage(promotion.getDiscountPercentage());
            existing.setDiscountAmount(promotion.getDiscountAmount());
            existing.setStartDate(promotion.getStartDate());
            existing.setEndDate(promotion.getEndDate());
            existing.setIsActive(promotion.getIsActive());
            existing.setProducts(promotion.getProducts());
            existing.setUpdatedAt(LocalDateTime.now());
            return promotionRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Promotion not found"));
    }

    public void delete(Long id) {
        promotionRepository.deleteById(id);
    }

    // ==========================
    // Method validatePromotion
    // ==========================
    public Promotion validatePromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        LocalDate today = LocalDate.now();
        if (!Boolean.TRUE.equals(promotion.getIsActive())
            || (promotion.getStartDate() != null && promotion.getStartDate().isAfter(today))
            || (promotion.getEndDate() != null && promotion.getEndDate().isBefore(today))) {
            throw new RuntimeException("Promotion is not active");
        }

        return promotion;
    }
}
