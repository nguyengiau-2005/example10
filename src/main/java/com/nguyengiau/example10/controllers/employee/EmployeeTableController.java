package com.nguyengiau.example10.controllers.employee;

import com.nguyengiau.example10.cafe.entity.Reservation;
import com.nguyengiau.example10.cafe.entity.TableEntity;
import com.nguyengiau.example10.cafe.entity.enums.Status;
import com.nguyengiau.example10.repository.ReservationRepository;
import com.nguyengiau.example10.repository.TableRepository;
import com.nguyengiau.example10.security.services.TableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/employee/tables")
@CrossOrigin(origins = "http://localhost:3000") // Cho phép frontend nhân viên truy cập
public class EmployeeTableController {

    private final TableRepository tableRepository;
    private final ReservationRepository reservationRepository;
    private final TableService tableService;

    // ✅ Constructor inject đầy đủ 3 dependency
    public EmployeeTableController(
            TableRepository tableRepository,
            ReservationRepository reservationRepository,
            TableService tableService
    ) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
        this.tableService = tableService;
    }

    // ========================== DANH SÁCH BÀN ==========================
    @GetMapping
    public List<TableEntity> getAll(@RequestParam(required = false) Optional<String> status) {
        return status.map(tableService::getByStatus).orElseGet(tableService::getAll);
    }

    // ========================== LẤY CHI TIẾT BÀN ==========================
    @GetMapping("/{id}")
    public ResponseEntity<TableEntity> getById(@PathVariable Long id) {
        return tableService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================== CẬP NHẬT TRẠNG THÁI ==========================
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body("Missing 'status' field");
        }

        try {
            TableEntity updated = tableService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ========================== KHÁCH GỌI MÓN TRỰC TIẾP ==========================
    @PostMapping("/order/start/{tableId}")
    public ResponseEntity<?> startOrder(@PathVariable Long tableId) {
        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Bàn không tồn tại"));

        if (table.getStatus() == Status.FREE) {
            table.setStatus(Status.OCCUPIED);
            tableRepository.save(table);
            return ResponseEntity.ok("✅ Bàn " + table.getNumber() + " đã chuyển sang OCCUPIED (đang phục vụ).");
        } else {
            return ResponseEntity.badRequest().body("⚠️ Bàn không ở trạng thái FREE để bắt đầu order!");
        }
    }

    // ========================== THANH TOÁN ==========================
    @PostMapping("/order/paid/{tableId}")
    public ResponseEntity<?> markAsPaid(@PathVariable Long tableId) {
        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Bàn không tồn tại"));

        if (table.getStatus() == Status.OCCUPIED) {
            table.setStatus(Status.PAID);
            tableRepository.save(table);
            return ResponseEntity.ok("✅ Bàn " + table.getNumber() + " đã thanh toán xong (PAID).");
        } else {
            return ResponseEntity.badRequest().body("⚠️ Bàn chưa ở trạng thái OCCUPIED để thanh toán!");
        }
    }

    // ========================== DỌN BÀN SAU THANH TOÁN ==========================
    @PostMapping("/order/cleanup/{tableId}")
    public ResponseEntity<?> cleanUpTable(@PathVariable Long tableId) {
        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Bàn không tồn tại"));

        if (table.getStatus() == Status.PAID) {
            table.setStatus(Status.FREE);
            tableRepository.save(table);
            return ResponseEntity.ok("✅ Bàn " + table.getNumber() + " đã được dọn xong (FREE).");
        } else {
            return ResponseEntity.badRequest().body("⚠️ Bàn chưa ở trạng thái PAID để dọn!");
        }
    }

    // ========================== NHÂN VIÊN XÁC NHẬN ĐẶT BÀN ==========================
    @PostMapping("/reservations/{reservationId}/confirm")
    public ResponseEntity<?> confirmReservation(@PathVariable Long reservationId, @RequestParam Long tableId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation không tồn tại"));

        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Bàn không tồn tại"));

        if (table.getStatus() == Status.FREE) {
            reservation.setStatus("CONFIRMED");
            reservation.setAssignedTableId(tableId);
            reservationRepository.save(reservation);

            table.setStatus(Status.RESERVED);
            tableRepository.save(table);

            return ResponseEntity.ok("✅ Đã xác nhận đặt bàn và chuyển bàn " + table.getNumber() + " sang RESERVED.");
        } else {
            return ResponseEntity.badRequest().body("⚠️ Bàn này không ở trạng thái FREE để đặt!");
        }
    }

    // ========================== KHÁCH ĐẾN NHẬN BÀN ==========================
    @PostMapping("/reservations/{reservationId}/arrived")
    public ResponseEntity<?> customerArrived(@PathVariable Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation không tồn tại"));

        if (reservation.getAssignedTableId() == null) {
            return ResponseEntity.badRequest().body("⚠️ Reservation chưa được gán bàn!");
        }

        TableEntity table = tableRepository.findById(reservation.getAssignedTableId())
                .orElseThrow(() -> new RuntimeException("Bàn không tồn tại!"));

        if (table.getStatus() == Status.RESERVED) {
            table.setStatus(Status.OCCUPIED);
            tableRepository.save(table);

            reservation.setStatus("OCCUPIED");
            reservationRepository.save(reservation);

            return ResponseEntity.ok("✅ Khách đã đến, bàn " + table.getNumber() + " chuyển sang OCCUPIED.");
        } else {
            return ResponseEntity.badRequest().body("⚠️ Bàn không ở trạng thái RESERVED!");
        }
    }

    // ========================== THANH TOÁN SAU ĐẶT TRƯỚC ==========================
    @PostMapping("/reservations/{reservationId}/paid")
    public ResponseEntity<?> reservationPaid(@PathVariable Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation không tồn tại"));

        if (reservation.getAssignedTableId() == null) {
            return ResponseEntity.badRequest().body("⚠️ Reservation chưa được gán bàn!");
        }

        TableEntity table = tableRepository.findById(reservation.getAssignedTableId())
                .orElseThrow(() -> new RuntimeException("Bàn không tồn tại!"));

        if (table.getStatus() == Status.OCCUPIED) {
            table.setStatus(Status.PAID);
            tableRepository.save(table);

            reservation.setStatus("PAID");
            reservationRepository.save(reservation);

            return ResponseEntity.ok("✅ Khách đã thanh toán, bàn " + table.getNumber() + " chuyển sang PAID.");
        } else {
            return ResponseEntity.badRequest().body("⚠️ Bàn chưa ở trạng thái OCCUPIED để thanh toán!");
        }
    }

    // ========================== DỌN BÀN SAU ĐẶT TRƯỚC ==========================
    @PostMapping("/reservations/{reservationId}/cleanup")
    public ResponseEntity<?> cleanupAfterReservation(@PathVariable Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation không tồn tại"));

        if (reservation.getAssignedTableId() == null) {
            return ResponseEntity.badRequest().body("⚠️ Reservation chưa được gán bàn!");
        }

        TableEntity table = tableRepository.findById(reservation.getAssignedTableId())
                .orElseThrow(() -> new RuntimeException("Bàn không tồn tại!"));

        if (table.getStatus() == Status.PAID) {
            table.setStatus(Status.FREE);
            tableRepository.save(table);

            reservation.setStatus("DONE");
            reservationRepository.save(reservation);

            return ResponseEntity.ok("✅ Dọn bàn " + table.getNumber() + " xong, bàn trở lại FREE.");
        } else {
            return ResponseEntity.badRequest().body("⚠️ Bàn chưa ở trạng thái PAID để dọn!");
        }
    }
}
