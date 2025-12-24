package com.nguyengiau.example10.repository;

import com.nguyengiau.example10.cafe.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nguyengiau.example10.cafe.entity.TableEntity;
import com.nguyengiau.example10.cafe.entity.enums.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Lấy order hiện tại của bàn theo trạng thái cụ thể
    Optional<Order> findByTableAndStatus(TableEntity table, OrderStatus status);

    // Lấy tất cả order theo trạng thái
    List<Order> findByStatus(OrderStatus status);

    // Lấy order gần nhất của bàn, bất kể trạng thái
    Optional<Order> findTopByTableOrderByCreatedAtDesc(TableEntity table);

    // Lấy order hiện tại của bàn, trừ trạng thái PAID và CANCELLED
    Optional<Order> findFirstByTableAndStatusNotInOrderByCreatedAtDesc(
        TableEntity table, List<OrderStatus> excludedStatuses
    );
    Optional<Order> findFirstByTableAndStatusOrderByCreatedAtDesc(TableEntity table, OrderStatus status);
List<Order> findAllByTableAndStatus(TableEntity table, OrderStatus status);

}
