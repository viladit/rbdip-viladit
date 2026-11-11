package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Намеренно нет @EntityGraph/JOIN FETCH-варианта - используется в цикле
    // контроллером, что и создаёт N+1 (см. OrderController#listOrders).
    @Query("select item from OrderItem item where item.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
}
