package com.example.cartrecovery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SmsNotConfiguredException.class)
    public ResponseEntity<Map<String, Object>> handleSmsNotConfigured(
            SmsNotConfiguredException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", 503,
                "error", "Service Unavailable",
                "code", "SMS_NOT_CONFIGURED",
                "message", exception.getMessage()
        ));
    }

    @ExceptionHandler({
            AbandonedCartNotFoundException.class,
            CustomerNotFoundException.class,
            CartNotFoundException.class,
            ProductNotFoundException.class,
            CartItemNotFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleNotFound(
            RuntimeException exception) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", 404);
        response.put("error", "Not Found");
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException exception) {

        Map<String, Object> response = new HashMap<>();
        String message = exception.getMessage();

        if (message != null && message.contains("not found")) {
            response.put("status", 404);
            response.put("error", "Not Found");
        } else {
            response.put("status", 400);
            response.put("error", "Bad Request");
        }

        response.put("message", message);

        HttpStatus status = message != null && message.contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException exception) {

        boolean mailFailure = exception.getCause() instanceof MailException;
        HttpStatus status = mailFailure
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.CONFLICT;

        Map<String, Object> response = new HashMap<>();
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", exception.getMessage());

        return ResponseEntity.status(status).body(response);
    }
}
