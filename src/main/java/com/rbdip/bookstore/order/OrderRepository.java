package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            select o from Order o
            join fetch o.customer
            left join fetch o.items item
            left join fetch item.product
            order by o.id
            """)
    List<Order> findAllWithItems();
}
