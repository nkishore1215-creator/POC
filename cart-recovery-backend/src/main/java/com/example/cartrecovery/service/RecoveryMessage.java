package com.example.cartrecovery.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Personalized copy plus the trusted cart facts used to render a recovery email.
 */
public record RecoveryMessage(
        String subject,
        String headline,
        String message,
        List<ProductSummary> products
) {
    public record ProductSummary(
            String name,
            String category,
            BigDecimal unitPrice,
            int quantity,
            long lastMonthCustomerCount,
            long lastMonthUnitsPurchased,
            boolean highSales
    ) {
    }
}
