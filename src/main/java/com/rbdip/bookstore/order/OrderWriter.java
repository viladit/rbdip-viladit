package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
class OrderWriter {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    OrderWriter(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    Order save(CreateOrderRequest request, List<OrderLine> lines) {
        Order order = orderRepository.save(new Order(
                request.customerFullName(), request.customerAddress(), request.customerPhone(), "new"));
        for (OrderLine line : lines) {
            orderItemRepository.save(new OrderItem(order.getId(), line.productName(), line.unitPrice(), line.quantity()));
        }
        return order;
    }
}
