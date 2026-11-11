package com.rbdip.bookstore.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = "SELECT EXISTS (SELECT 1 FROM order_items)", nativeQuery = true)
    boolean hasRecordedPurchases();
}
