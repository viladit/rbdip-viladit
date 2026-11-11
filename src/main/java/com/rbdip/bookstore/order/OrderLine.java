package com.rbdip.bookstore.order;

import java.math.BigDecimal;

record OrderLine(String productName, BigDecimal unitPrice, int quantity) {
}
