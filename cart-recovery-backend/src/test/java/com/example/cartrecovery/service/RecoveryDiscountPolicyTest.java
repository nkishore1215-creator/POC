package com.example.cartrecovery.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecoveryDiscountPolicyTest {

    private final RecoveryDiscountPolicy policy = new RecoveryDiscountPolicy(
            new BigDecimal("500"),
            new BigDecimal("1000"),
            new BigDecimal("2000")
    );

    @Test
    void selectsDiscountPercentageFromCartTotal() {
        assertEquals(0, policy.percentageFor(BigDecimal.ZERO));
        assertEquals(0, policy.percentageFor(new BigDecimal("499.99")));
        assertEquals(2, policy.percentageFor(new BigDecimal("500.00")));
        assertEquals(2, policy.percentageFor(new BigDecimal("999.99")));
        assertEquals(5, policy.percentageFor(new BigDecimal("1000.00")));
        assertEquals(5, policy.percentageFor(new BigDecimal("1999.99")));
        assertEquals(10, policy.percentageFor(new BigDecimal("2000.00")));
        assertEquals(10, policy.percentageFor(new BigDecimal("2500.00")));
    }

    @Test
    void appliesDiscountAndRoundsToCurrencyPrecision() {
        assertEquals(
                new BigDecimal("1800.00"),
                policy.apply(new BigDecimal("2000.00"), 10)
        );
    }
}
