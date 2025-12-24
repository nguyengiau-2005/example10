package com.nguyengiau.example10.dto;

import lombok.Data;

@Data
public class OrderRequestItemDTO {
    private Long productId;
    private Integer quantity;
    private Double price;
}
