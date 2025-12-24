package com.nguyengiau.example10.dto;

import lombok.Data;

@Data
public class AddProductRequest {
    private Long tableId;
    private Long productId;
    private int quantity;
}
