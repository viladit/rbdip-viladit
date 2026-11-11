package com.rbdip.bookstore.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest
    @CsvSource({"10, 100.00", "11, 104.50"})
    void appliesBulkDiscountAboveTenUnits(int quantity, String expected) {
        BigDecimal total = calculator.calculateOrderTotal(List.of(line("10.00", quantity)), "regular", null);

        assertEquals(new BigDecimal(expected), total);
    }

    @Test
    void appliesBulkDiscountToEachLineIndependently() {
        BigDecimal total = calculator.calculateOrderTotal(
                List.of(line("10.00", 6), line("10.00", 5)), "regular", null);

        assertEquals(new BigDecimal("110.00"), total);
    }

    @ParameterizedTest
    @CsvSource({"vip, 80.00", "wholesale, 75.00"})
    void appliesCustomerDiscountBeforeFixedCoupon(String customerType, String expected) {
        BigDecimal total = calculator.calculateOrderTotal(List.of(line("100.00", 1)), customerType, "SAVE10");

        assertEquals(new BigDecimal(expected), total);
    }

    @ParameterizedTest
    @CsvSource({"1250.00, 1000.00", "1250.01, 980.01"})
    void checksLargeOrderThresholdAfterPercentageCoupon(String price, String expected) {
        BigDecimal total = calculator.calculateOrderTotal(List.of(line(price, 1)), "regular", "SAVE20PERCENT");

        assertEquals(new BigDecimal(expected), total);
    }

    @Test
    void floorsNegativeTotalFromFixedCouponAtZero() {
        BigDecimal total = calculator.calculateOrderTotal(List.of(line("5.00", 1)), null, "SAVE10");

        assertEquals(new BigDecimal("0.00"), total);
    }

    @Test
    void floorsNegativeQuantityAtZero() {
        BigDecimal total = calculator.calculateOrderTotal(List.of(line("10.00", -1)), "regular", null);

        assertEquals(new BigDecimal("0.00"), total);
    }

    @Test
    void roundsHalfCentUpAfterDiscounts() {
        BigDecimal total = calculator.calculateOrderTotal(List.of(line("0.15", 1)), "vip", null);

        assertEquals(new BigDecimal("0.14"), total);
    }

    @Test
    void leavesPriceUnchangedWithoutCustomerTypeOrCoupon() {
        BigDecimal total = calculator.calculateOrderTotal(List.of(line("10.00", 1)), null, null);

        assertEquals(new BigDecimal("10.00"), total);
    }

    private static PricingCalculator.LineItem line(String price, int quantity) {
        return new PricingCalculator.LineItem(new BigDecimal(price), quantity);
    }
}
