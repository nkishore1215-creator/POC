package com.example.cartrecovery.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long cartItemId) {
        super("Cart item not found with ID: " + cartItemId);
    }
}
