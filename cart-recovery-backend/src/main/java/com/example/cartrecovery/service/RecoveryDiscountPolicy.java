package com.example.cartrecovery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RecoveryDiscountPolicy {

    private final BigDecimal twoPercentMinimum;
    private final BigDecimal fivePercentMinimum;
    private final BigDecimal tenPercentMinimum;

    public RecoveryDiscountPolicy(
            @Value("${abandoned.cart.discount.two-percent-minimum:500}")
            BigDecimal twoPercentMinimum,
            @Value("${abandoned.cart.discount.five-percent-minimum:1000}")
            BigDecimal fivePercentMinimum,
            @Value("${abandoned.cart.discount.ten-percent-minimum:2000}")
            BigDecimal tenPercentMinimum
    ) {
        this.twoPercentMinimum = twoPercentMinimum;
        this.fivePercentMinimum = fivePercentMinimum;
        this.tenPercentMinimum = tenPercentMinimum;
    }

    public int percentageFor(BigDecimal cartTotal) {
        if (cartTotal == null || cartTotal.compareTo(twoPercentMinimum) < 0) {
            return 0;
        }
        if (cartTotal.compareTo(tenPercentMinimum) >= 0) {
            return 10;
        }
        if (cartTotal.compareTo(fivePercentMinimum) >= 0) {
            return 5;
        }
        return 2;
    }

    public BigDecimal apply(BigDecimal cartTotal, int percentage) {
        if (cartTotal == null || percentage <= 0) {
            return cartTotal;
        }
        BigDecimal discount = cartTotal
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return cartTotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }
}
