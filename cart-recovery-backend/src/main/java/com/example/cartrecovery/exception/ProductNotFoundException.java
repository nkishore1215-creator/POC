package com.example.cartrecovery.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Product not found with ID: " + productId);
    }

    public ProductNotFoundException(String productName) {
        super("Product not found with name: " + productName);
    }
}
