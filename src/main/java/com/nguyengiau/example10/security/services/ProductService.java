package com.nguyengiau.example10.security.services;

import com.nguyengiau.example10.cafe.entity.Product;
import com.nguyengiau.example10.dto.ProductDto;
import com.nguyengiau.example10.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    // ✅ Lấy toàn bộ sản phẩm (Product entity)
    public List<Product> getAll() {
        return repo.findAll();
    }

    // ✅ Lấy toàn bộ sản phẩm dưới dạng DTO (có imageUrl)
    public List<ProductDto> getAllDto() {
        return repo.findAll().stream()
                .map(p -> new ProductDto(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getPrice(),
                        p.getImageUrl(), // tên file trong static/images/
                        p.getCategory() != null ? p.getCategory().getName() : null
                ))
                .collect(Collectors.toList());
    }

    // ✅ Tìm kiếm theo tên
    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return repo.findAll();
        }
        return repo.findByNameContainingIgnoreCase(keyword);
    }

    // ✅ Lọc theo danh mục + tìm kiếm
    public List<Product> getFiltered(String category, String keyword) {
        return repo.findAll().stream()
                .filter(p -> category == null
                        || (p.getCategory() != null
                            && p.getCategory().getName() != null
                            && p.getCategory().getName().equalsIgnoreCase(category)))
                .filter(p -> keyword == null
                        || p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    // ✅ Lấy sản phẩm theo ID (Optional<Product>)
    public Optional<Product> getById(Long id) {
        return repo.findById(id);
    }

    // ✅ Lấy sản phẩm theo ID (Product, nếu không có thì throw)
    public Product findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
    }

    // ✅ Thêm mới sản phẩm
    public Product create(Product product) {
        return repo.save(product);
    }

    // ✅ Cập nhật sản phẩm
    public Product update(Long id, Product product) {
        return repo.findById(id).map(p -> {
            p.setName(product.getName());
            p.setDescription(product.getDescription());
            p.setImageUrl(product.getImageUrl()); // ví dụ "h1.jpg"
            p.setPrice(product.getPrice());
            p.setCategory(product.getCategory());
            p.setPromotions(product.getPromotions());
            return repo.save(p);
        }).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    // ✅ Xóa sản phẩm
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
