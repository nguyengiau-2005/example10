package com.nguyengiau.example10.dto;

import com.nguyengiau.example10.cafe.entity.enums.PaymentMethod;

public class PaymentRequest {
    private Long orderId;
    private PaymentMethod method;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
}
