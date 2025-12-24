package com.nguyengiau.example10.security.services;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nguyengiau.example10.cafe.entity.*;
import com.nguyengiau.example10.cafe.entity.enums.OrderStatus;
import com.nguyengiau.example10.cafe.entity.enums.PaymentMethod;
import com.nguyengiau.example10.cafe.entity.enums.PaymentStatus;
import com.nguyengiau.example10.exception.InvalidStatusTransitionException;
import com.nguyengiau.example10.exception.NotFoundException;
import com.nguyengiau.example10.repository.*;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final TableRepository tableRepo;
    private final EmployeeRepository employeeRepo;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(OrderRepository orderRepo,
                        ProductRepository productRepo,
                        TableRepository tableRepo,
                        EmployeeRepository employeeRepo,
                        PaymentRepository paymentRepository,
                        OrderItemRepository orderItemRepository) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.tableRepo = tableRepo;
        this.employeeRepo = employeeRepo;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // ------------------ TABLE ------------------
    public Optional<TableEntity> getTableById(Long id) {
        return tableRepo.findById(id);
    }

    public Order save(Order order) {
        return orderRepo.save(order);
    }

    // ------------------ PRODUCT ------------------
    public Optional<Product> getProductById(Long id) {
        return productRepo.findById(id);
    }

    // ------------------ ORDER ------------------
    public List<Order> getAll() {
        return orderRepo.findAll();
    }

    public Optional<Order> getById(Long id) {
        return orderRepo.findById(id);
    }

    public Order createOrderForTable(TableEntity table, List<OrderItem> items) {
        Order order = new Order();
        order.setTable(table);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            Product product = productRepo.findById(item.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + item.getProduct().getId()));

            item.setPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            item.setOrder(order);

            order.getItems().add(item);
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);
        return orderRepo.save(order);
    }

    public Order updateStatus(Long orderId, OrderStatus nextStatus, Long employeeId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (!isValidStatusTransition(order.getStatus(), nextStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot change status from " + order.getStatus() + " to " + nextStatus
            );
        }

        order.setStatus(nextStatus);
        order.setUpdatedAt(LocalDateTime.now());

        if (employeeId != null) {
            employeeRepo.findById(employeeId).ifPresent(order::setEmployee);
        }

        return orderRepo.save(order);
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED;
            case CONFIRMED -> next == OrderStatus.PREPARING;
            case PREPARING -> next == OrderStatus.SERVED;
            case SERVED -> next == OrderStatus.PAID;
            case PAID, CANCELLED -> false;
        };
    }

    public void delete(Long id) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
        orderRepo.delete(order);
    }

    // ------------------ PAYMENT ------------------
    public Payment createPayment(Long orderId, PaymentMethod method) {
        return createPayment(orderId, method, null);
    }

    public Payment createPayment(Long orderId, PaymentMethod method, String mobileCode) {
        Order order = getById(orderId)
                .orElseThrow(() -> new NotFoundException("Order không tồn tại: " + orderId));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setAmount(order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        if (method == PaymentMethod.MOBILE) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setMobileCode(generateMobileCode());
        } else {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setMobileCode(null);
        }

        return paymentRepository.save(payment);
    }

    private String generateMobileCode() {
        return "MM" + System.currentTimeMillis();
    }

    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        payment.setStatus(status);
        return paymentRepository.save(payment);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Order> getCurrentOrdersOfTable(TableEntity table) {
        return orderRepo.findAllByTableAndStatus(table, OrderStatus.PENDING);
    }

    public Payment getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            throw new NotFoundException("Payment không tồn tại cho orderId: " + orderId);
        }
        return payment;
    }

    // ------------------ PDF ------------------
    public byte[] exportInvoicePdf(Long orderId, PaymentMethod method, String paymentCode) {
        try {
            Order order = getById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // --- Logo ---
            try {
                Image logo = new Image(ImageDataFactory.create(getClass().getResource("/logo.png")));
                logo.setWidth(100);
                document.add(logo);
            } catch (Exception e) {}

            // --- Header ---
            Paragraph header = new Paragraph("HÓA ĐƠN THANH TOÁN")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(header);
            document.add(new Paragraph("Order ID: " + order.getId()));
            document.add(new Paragraph("Bàn: " + order.getTable().getNumber()));
            document.add(new Paragraph("Nhân viên: " + (order.getEmployee() != null ? order.getEmployee().getUsername() : "Chưa có")));
            document.add(new Paragraph("\n"));

            // --- Bảng sản phẩm ---
            Table table = new Table(UnitValue.createPercentArray(new float[]{4,1,2,2})).useAllAvailableWidth();
            table.addHeaderCell(new Cell().add(new Paragraph("Sản phẩm").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("SL").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Đơn giá").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Thành tiền").setBold()));

            BigDecimal total = BigDecimal.ZERO;
            for (OrderItem item : order.getItems()) {
                BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(subtotal);
                table.addCell(new Cell().add(new Paragraph(item.getProduct().getName())));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity()))));
                table.addCell(new Cell().add(new Paragraph(item.getPrice().toString())));
                table.addCell(new Cell().add(new Paragraph(subtotal.toString())));
            }
            document.add(table);

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("TỔNG: " + total + " VNĐ")
                    .setBold()
                    .setFontSize(14)
                    .setFontColor(ColorConstants.RED));
            document.add(new Paragraph("Phương thức thanh toán: " + method));

            // QR code cho MOBILE
            if (method == PaymentMethod.MOBILE && paymentCode != null) {
                document.add(new Paragraph("Mã thanh toán: " + paymentCode));
                try {
                    QRCodeWriter qrWriter = new QRCodeWriter();
                    BitMatrix bitMatrix = qrWriter.encode(paymentCode, BarcodeFormat.QR_CODE, 150, 150);
                    BufferedImage qrImage = new BufferedImage(150,150,BufferedImage.TYPE_INT_RGB);
                    for(int x=0;x<150;x++){
                        for(int y=0;y<150;y++){
                            qrImage.setRGB(x,y, bitMatrix.get(x,y) ? 0xFF000000 : 0xFFFFFFFF);
                        }
                    }
                    ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
                    ImageIO.write(qrImage,"PNG",qrBaos);
                    Image qr = new Image(ImageDataFactory.create(qrBaos.toByteArray()));

                    // Căn giữa QR code
                    Paragraph p = new Paragraph();
                    p.add(qr);
                    p.setTextAlignment(TextAlignment.CENTER);
                    document.add(p);
                } catch(Exception e){ e.printStackTrace(); }
            }

            document.close();
            return baos.toByteArray();
        } catch(Exception e) {
            throw new RuntimeException("Lỗi xuất PDF: "+e.getMessage(), e);
        }
    }

    // ------------------ THÊM SẢN PHẨM ------------------
 public Order addProductToTable(TableEntity table, Product product, int quantity) {
    // 1️⃣ Lấy order PENDING nếu có, hoặc tạo mới
    Order order = orderRepo.findAllByTableAndStatus(table, OrderStatus.PENDING)
            .stream()
            .findFirst()
            .orElseGet(() -> {
                Order newOrder = new Order();
                newOrder.setTable(table);
                newOrder.setStatus(OrderStatus.PENDING);
                newOrder.setCreatedAt(LocalDateTime.now());
                newOrder.setUpdatedAt(LocalDateTime.now());
                newOrder.setTotalAmount(BigDecimal.ZERO);
                newOrder.setItems(new ArrayList<>());
                return orderRepo.save(newOrder);
            });

    // 2️⃣ Tìm món đã có trong order
    Optional<OrderItem> existingItem = order.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(product.getId()))
            .findFirst();

    if (existingItem.isPresent()) {
        // 3️⃣ Nếu món đã có, tăng số lượng
        OrderItem item = existingItem.get();
        item.setQuantity(item.getQuantity() + quantity);
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        item.setUpdatedAt(LocalDateTime.now());
        orderItemRepository.save(item);
    } else {
        // 4️⃣ Nếu món mới, thêm vào order
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPrice(product.getPrice());
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        order.getItems().add(item);
        orderItemRepository.save(item);
    }

    // 5️⃣ Cập nhật tổng tiền
    BigDecimal total = order.getItems().stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    order.setTotalAmount(total);
    order.setUpdatedAt(LocalDateTime.now());

    // 6️⃣ Trả về order với items đã load đầy đủ
    order = orderRepo.findById(order.getId())
            .orElseThrow(() -> new RuntimeException("Order not found after save"));

    return order;
}

    public Order advanceOrderStatus(Long orderId, OrderStatus nextStatus, Long employeeId) {
        return updateStatus(orderId, nextStatus, employeeId);
    }

    public Order getCurrentOrderOfTable(TableEntity table) {
        return orderRepo.findByTableAndStatus(table, OrderStatus.PENDING)
                .orElseGet(() -> {
                    Order newOrder = new Order();
                    newOrder.setTable(table);
                    newOrder.setStatus(OrderStatus.PENDING);
                    newOrder.setCreatedAt(LocalDateTime.now());
                    newOrder.setUpdatedAt(LocalDateTime.now());
                    newOrder.setTotalAmount(BigDecimal.ZERO);
                    newOrder.setItems(new ArrayList<>());
                    return orderRepo.save(newOrder);
                });
    }
    public byte[] exportInvoicePdf(Long orderId) {
    Order order = getById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));

    Payment payment = getPaymentByOrderId(orderId);
    PaymentMethod method = payment != null ? payment.getMethod() : null;
    String code = payment != null ? payment.getMobileCode() : null;

    return exportInvoicePdf(orderId, method, code); // gọi phương thức export chính
}

}
