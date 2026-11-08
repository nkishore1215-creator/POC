package com.example.cartrecovery.service;

/** Implement as a Spring bean for the chosen SMS provider. */
@FunctionalInterface
public interface SmsSender {
    /** Returns when the provider accepts the message; throws on failure. */
    void send(String phoneNumber, String message);
}
