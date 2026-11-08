package com.example.cartrecovery.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long customerId) {
        super("Customer not found with ID: " + customerId);
    }

    public CustomerNotFoundException(String email) {
        super("Customer not found with email: " + email);
    }
}
