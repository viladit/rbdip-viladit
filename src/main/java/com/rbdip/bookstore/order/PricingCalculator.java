package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PricingCalculator {

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal BULK_PRICE_FACTOR = new BigDecimal("0.95");
    private static final String VIP_CUSTOMER_TYPE = "vip";
    private static final BigDecimal VIP_PRICE_FACTOR = new BigDecimal("0.9");
    private static final String WHOLESALE_CUSTOMER_TYPE = "wholesale";
    private static final BigDecimal WHOLESALE_PRICE_FACTOR = new BigDecimal("0.85");
    private static final String SAVE10_COUPON = "SAVE10";
    private static final BigDecimal SAVE10_AMOUNT = BigDecimal.TEN;
    private static final String SAVE20PERCENT_COUPON = "SAVE20PERCENT";
    private static final BigDecimal SAVE20PERCENT_PRICE_FACTOR = new BigDecimal("0.8");
    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_ORDER_PRICE_FACTOR = new BigDecimal("0.98");
    private static final int MONEY_SCALE = 2;

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_QUANTITY_THRESHOLD) {
                linePrice = linePrice.multiply(BULK_PRICE_FACTOR);
            }
            total = total.add(linePrice);
        }

        if (VIP_CUSTOMER_TYPE.equals(customerType)) {
            total = total.multiply(VIP_PRICE_FACTOR);
        } else if (WHOLESALE_CUSTOMER_TYPE.equals(customerType)) {
            total = total.multiply(WHOLESALE_PRICE_FACTOR);
        }

        if (SAVE10_COUPON.equals(couponCode)) {
            total = total.subtract(SAVE10_AMOUNT);
        } else if (SAVE20PERCENT_COUPON.equals(couponCode)) {
            total = total.multiply(SAVE20PERCENT_PRICE_FACTOR);
        }

        total = total.max(BigDecimal.ZERO);

        if (total.compareTo(LARGE_ORDER_THRESHOLD) > 0) {
            total = total.multiply(LARGE_ORDER_PRICE_FACTOR);
        }

        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
