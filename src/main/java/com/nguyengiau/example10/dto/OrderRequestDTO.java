package com.nguyengiau.example10.dto;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequestDTO {
    private Long tableId;
    private String customerName;
    private List<OrderRequestItemDTO> items;
}