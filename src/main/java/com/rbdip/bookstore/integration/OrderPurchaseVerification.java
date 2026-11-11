package com.rbdip.bookstore.integration;

import com.rbdip.bookstore.order.OrderItemRepository;
import com.rbdip.bookstore.review.PurchaseVerification;
import org.springframework.stereotype.Component;

@Component
class OrderPurchaseVerification implements PurchaseVerification {

    private final OrderItemRepository orderItemRepository;

    OrderPurchaseVerification(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasRecordedPurchases() {
        return orderItemRepository.hasRecordedPurchases();
    }
}
