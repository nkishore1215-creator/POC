package com.example.cartrecovery.exception;

public class AbandonedCartNotFoundException extends RuntimeException {

    public AbandonedCartNotFoundException(Long cartId) {

        super("Abandoned cart not found for cart ID: " + cartId);
    }
}
