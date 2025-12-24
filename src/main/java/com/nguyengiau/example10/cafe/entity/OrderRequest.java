package com.nguyengiau.example10.cafe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_requests")
public class OrderRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private TableEntity table;

    private String customerName;

    @JsonManagedReference(value = "orderRequest-items")
    @OneToMany(mappedBy = "orderRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderRequestItem> items = new ArrayList<>();

    private boolean confirmed = false;

    private LocalDateTime createdAt;

    // Helper để tự gán 2 chiều
    public void addItem(OrderRequestItem item) {
        items.add(item);
        item.setOrderRequest(this);
    }
}
