package com.nguyengiau.example10.controllers;

import com.nguyengiau.example10.cafe.entity.*;
import com.nguyengiau.example10.cafe.entity.enums.OrderStatus;
import com.nguyengiau.example10.cafe.entity.enums.PaymentMethod;
import com.nguyengiau.example10.cafe.entity.enums.PaymentStatus;
import com.nguyengiau.example10.security.services.OrderService;
import com.nguyengiau.example10.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public static class CreateOrderRequest {
        public Long tableId;
        public List<OrderItemDTO> items;
    }

    public static class OrderItemDTO {
        public Long productId;
        public int quantity;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest req) {
        TableEntity table = orderService.getTableById(req.tableId)
                .orElseThrow(() -> new NotFoundException("Bàn không tồn tại: " + req.tableId));

        List<OrderItem> items = req.items.stream().map(i -> {
            Product product = orderService.getProductById(i.productId)
                    .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại: " + i.productId));
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(i.quantity);
            return item;
        }).collect(Collectors.toList());

        Order created = orderService.createOrderForTable(table, items);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) Long employeeId) {

        OrderStatus orderStatus;
        try {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Trạng thái không hợp lệ: " + status);
        }

        Order updated = orderService.updateStatus(id, orderStatus, employeeId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<Payment> createPayment(@PathVariable Long id,
                                                 @RequestParam PaymentMethod method) {
        Payment payment = orderService.createPayment(id, method);
        return ResponseEntity.ok(payment);
    }

    @PutMapping("/payment/{paymentId}/status")
    public ResponseEntity<Payment> updatePaymentStatus(@PathVariable Long paymentId,
                                                       @RequestParam PaymentStatus status) {
        Payment payment = orderService.updatePaymentStatus(paymentId, status);
        return ResponseEntity.ok(payment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        try {
            orderService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

@GetMapping("/{id}/invoice")
public ResponseEntity<byte[]> exportInvoice(@PathVariable Long id) {
    // Gọi phương thức của service
    byte[] pdf = orderService.exportInvoicePdf(id); // chỉ cần id, service tự lấy Payment, QR code

    return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=invoice_" + id + ".pdf")
            .body(pdf);
}
}
