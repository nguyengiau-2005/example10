package com.nguyengiau.example10.controllers.employee;

import com.nguyengiau.example10.cafe.entity.*;
import com.nguyengiau.example10.cafe.entity.enums.OrderStatus;
import com.nguyengiau.example10.cafe.entity.enums.PaymentMethod;
import com.nguyengiau.example10.cafe.entity.enums.PaymentStatus;
import com.nguyengiau.example10.exception.NotFoundException;
import com.nguyengiau.example10.repository.EmployeeRepository;
import com.nguyengiau.example10.repository.OrderRequestRepository;
import com.nguyengiau.example10.security.services.OrderService;
import com.nguyengiau.example10.security.services.ProductService;
import com.nguyengiau.example10.security.services.TableService;
import com.nguyengiau.example10.dto.AddProductRequest;
import com.nguyengiau.example10.dto.OrderItemDTO;
import com.nguyengiau.example10.dto.PaymentRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Base64;

@RestController
@RequestMapping("/api/employee")
@CrossOrigin(origins = "*")
public class EmployeeOrderController {

    private final OrderService orderService;
    private final OrderRequestRepository orderRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final TableService tableService;
    private final ProductService productService;

    public EmployeeOrderController(OrderService orderService,
                                   OrderRequestRepository orderRequestRepository,
                                   EmployeeRepository employeeRepository,
                                   TableService tableService,
                                   ProductService productService) {
        this.orderService = orderService;
        this.orderRequestRepository = orderRequestRepository;
        this.employeeRepository = employeeRepository;
        this.tableService = tableService;
        this.productService = productService;
    }

    // =================== ORDER ===================
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/orders/create")
    public ResponseEntity<Order> createOrder(@RequestParam Long tableId,
                                             @RequestBody List<OrderItemDTO> itemsDto) {
        TableEntity table = orderService.getTableById(tableId)
                .orElseThrow(() -> new NotFoundException("Bàn không tồn tại: " + tableId));

        List<OrderItem> items = itemsDto.stream().map(dto -> {
            Product product = orderService.getProductById(dto.getProductId())
                    .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại: " + dto.getProductId()));
            OrderItem oi = new OrderItem();
            oi.setProduct(product);
            oi.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);
            oi.setPrice(product.getPrice());
            oi.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
            return oi;
        }).toList();

        Order order = orderService.createOrderForTable(table, items);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/orders/add-product")
    public ResponseEntity<Order> addProductToTable(@RequestBody AddProductRequest req) {
        TableEntity table = tableService.getById(req.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn!"));
        Product product = productService.findById(req.getProductId());
        Order order = orderService.addProductToTable(table, product, req.getQuantity());
        return ResponseEntity.ok(order);
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> advanceOrderStatus(@PathVariable Long id,
                                                    @RequestParam OrderStatus nextStatus,
                                                    @RequestParam(required = false) Long employeeId) {
        Order order = orderService.advanceOrderStatus(id, nextStatus, employeeId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders/current")
    public ResponseEntity<List<Order>> getCurrentOrdersOfTable(@RequestParam Long tableId) {
        TableEntity table = orderService.getTableById(tableId)
                .orElseThrow(() -> new NotFoundException("Bàn không tồn tại: " + tableId));
        List<Order> orders = orderService.getCurrentOrdersOfTable(table);
        return ResponseEntity.ok(orders);
    }

    // =================== PAYMENT ===================
    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(orderService.getAllPayments());
    }

    @PutMapping("/payments/{id}/status")
    public ResponseEntity<Payment> updatePaymentStatus(@PathVariable Long id,
                                                       @RequestParam PaymentStatus status) {
        Payment payment = orderService.updatePaymentStatus(id, status);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/payments/status/{orderId}")
    public ResponseEntity<PaymentStatus> getPaymentStatus(@PathVariable Long orderId) {
        Payment payment = orderService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(payment.getStatus());
    }

    // =================== CHECKOUT ===================
@PostMapping("/orders/{orderId}/checkout")
public ResponseEntity<?> checkoutOrder(@PathVariable Long orderId,
                                       @RequestBody PaymentRequest request) {
    Payment payment = orderService.createPayment(orderId, request.getMethod(), "CODE123");
    byte[] pdfBytes = orderService.exportInvoicePdf(orderId, request.getMethod(), "CODE123");

    return ResponseEntity.ok(Map.of(
            "invoicePdfBase64", Base64.getEncoder().encodeToString(pdfBytes),
            "paymentCode", "CODE123",
            "paymentStatus", payment.getStatus(),
            "amount", payment.getAmount()
    ));
}

    // =================== CONFIRM ORDER REQUEST ===================
    @PutMapping("/order-requests/{id}/confirm")
    public ResponseEntity<Order> confirmOrderRequest(@PathVariable Long id, @RequestParam Long employeeId) {
        OrderRequest request = orderRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order request không tồn tại"));

        request.setConfirmed(true);
        orderRequestRepository.save(request);

        List<OrderItem> orderItems = request.getItems().stream().map(ri -> {
            OrderItem item = new OrderItem();
            item.setProduct(ri.getProduct());
            item.setQuantity(ri.getQuantity());
            item.setPrice(ri.getProduct().getPrice());
            item.setSubtotal(ri.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(ri.getQuantity())));
            return item;
        }).toList();

        Order order = orderService.createOrderForTable(request.getTable(), orderItems);
        order.setEmployee(employeeRepository.findById(employeeId).orElse(null));
        orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED, employeeId);

        return ResponseEntity.ok(order);
    }
}
