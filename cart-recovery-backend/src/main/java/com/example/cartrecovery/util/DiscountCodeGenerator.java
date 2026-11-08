package com.example.cartrecovery.util;

import java.security.SecureRandom;
import java.util.Locale;

public final class DiscountCodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private DiscountCodeGenerator() {
    }

    public static String generate(String prefix) {
        return prefix + "-" + String.format(Locale.ROOT, "%010d", RANDOM.nextLong(10_000_000_000L));
    }
}
