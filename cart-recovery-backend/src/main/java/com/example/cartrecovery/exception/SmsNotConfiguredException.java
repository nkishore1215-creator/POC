package com.example.cartrecovery.exception;

public class SmsNotConfiguredException extends IllegalStateException {
    public SmsNotConfiguredException() {
        super("SMS sending is unavailable because no SMS provider is configured.");
    }
}
