package com.nguyengiau.example10.controllers;

import com.nguyengiau.example10.cafe.entity.Category;
import com.nguyengiau.example10.cafe.entity.Product;
import com.nguyengiau.example10.dto.ProductDto;
import com.nguyengiau.example10.repository.CategoryRepository;
import com.nguyengiau.example10.security.services.ProductService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@CrossOrigin(origins = "http://localhost:3000") // Cho phép React truy cập
public class ProductController {

    private final ProductService service;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductService service, CategoryRepository categoryRepository) {
        this.service = service;
        this.categoryRepository = categoryRepository;
    }

    // ===============================
    // ✅ Lấy danh sách sản phẩm (có thể lọc theo danh mục + tìm kiếm)
    // ===============================
    @GetMapping
    public List<ProductDto> getFilteredProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return service.getFiltered(category, keyword).stream()
                .map(p -> new ProductDto(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getPrice(),
                        p.getImageUrl(), 
                        p.getCategory() != null ? p.getCategory().getName() : null
                ))
                .toList();
    }

    // ===============================
    // ✅ Lấy 1 sản phẩm theo ID
    // ===============================
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(p -> new ProductDto(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getPrice(),
                        p.getImageUrl(),
                        p.getCategory() != null ? p.getCategory().getName() : null
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===============================
    // ✅ Thêm mới sản phẩm
    // ===============================
    @PostMapping
    public ProductDto create(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam BigDecimal price,
            @RequestParam Integer stockQuantity,
            @RequestParam Long categoryId,
            @RequestParam String imageUrl // ví dụ "h1.jpg"
    ) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại"));

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        product.setImageUrl(imageUrl);

        Product created = service.create(product);
        return new ProductDto(
                created.getId(),
                created.getName(),
                created.getDescription(),
                created.getPrice(),
                created.getImageUrl(),
                created.getCategory() != null ? created.getCategory().getName() : null
        );
    }

    // ===============================
    // ✅ Cập nhật sản phẩm
    // ===============================
    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id, @RequestBody Product product) {
        Product updated = service.update(id, product);
        return new ProductDto(
                updated.getId(),
                updated.getName(),
                updated.getDescription(),
                updated.getPrice(),
                updated.getImageUrl(),
                updated.getCategory() != null ? updated.getCategory().getName() : null
        );
    }

    // ===============================
    // ✅ Xóa sản phẩm
    // ===============================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
