package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final String DEFAULT_CUSTOMER_TYPE = "regular";

    private final OrderRequestPreparer requestPreparer;
    private final PricingCalculator pricingCalculator;
    private final OrderWriter orderWriter;
    private final OrderConfirmation orderConfirmation;

    public OrderService(
            OrderRequestPreparer requestPreparer,
            PricingCalculator pricingCalculator,
            OrderWriter orderWriter,
            OrderConfirmation orderConfirmation) {
        this.requestPreparer = requestPreparer;
        this.pricingCalculator = pricingCalculator;
        this.orderWriter = orderWriter;
        this.orderConfirmation = orderConfirmation;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        List<OrderLine> lines = requestPreparer.prepare(request);
        List<PricingCalculator.LineItem> pricingItems = lines.stream()
                .map(line -> new PricingCalculator.LineItem(line.product().getPrice(), line.quantity()))
                .toList();
        String customerType = request.customerType() == null ? DEFAULT_CUSTOMER_TYPE : request.customerType();
        BigDecimal total = pricingCalculator.calculateOrderTotal(pricingItems, customerType, request.couponCode());

        Order order = orderWriter.save(request, lines);
        orderConfirmation.send(request.customerFullName(), order.getId(), total);
        return order;
    }
}
