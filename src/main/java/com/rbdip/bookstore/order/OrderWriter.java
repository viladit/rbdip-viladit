package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
class OrderWriter {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    OrderWriter(CustomerRepository customerRepository, OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    Order save(CreateOrderRequest request, List<OrderLine> lines) {
        String fullName = request.customerFullName();
        String address = request.customerAddress();
        String phone = request.customerPhone();
        customerRepository.insertIfMissing(fullName, address, phone);
        Customer customer = customerRepository.findByFullNameAndAddressAndPhone(fullName, address, phone);
        Order order = orderRepository.save(new Order(customer, "new"));
        for (OrderLine line : lines) {
            orderItemRepository.save(new OrderItem(order, line.product(), line.quantity()));
        }
        return order;
    }
}
