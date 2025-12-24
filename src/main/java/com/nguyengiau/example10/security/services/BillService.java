package com.nguyengiau.example10.security.services;

import com.nguyengiau.example10.cafe.entity.Bill;
import com.nguyengiau.example10.repository.BillRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BillService {

    private final BillRepository repo;

    public BillService(BillRepository repo) {
        this.repo = repo;
    }

    public List<Bill> getAll() {
        return repo.findAll();
    }

    public Optional<Bill> getById(Long id) {
        return repo.findById(id);
    }

    public Bill create(Bill bill) {
        LocalDateTime now = LocalDateTime.now();
        bill.setCreatedAt(now);
        bill.setUpdatedAt(now);
        return repo.save(bill);
    }

    public Bill update(Long id, Bill bill) {
        return repo.findById(id).map(existing -> {
            existing.setTotalAmount(bill.getTotalAmount());
            existing.setPaymentMethod(bill.getPaymentMethod());
            existing.setPaymentStatus(bill.getPaymentStatus());
            existing.setIssuedAt(bill.getIssuedAt());
            existing.setNotes(bill.getNotes());
            existing.setUpdatedAt(LocalDateTime.now());
            return repo.save(existing);
        }).orElseThrow(() -> new RuntimeException("Bill not found with id " + id));
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
