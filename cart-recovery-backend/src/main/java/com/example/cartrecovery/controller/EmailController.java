package com.example.cartrecovery.controller;

import com.example.cartrecovery.entity.Notification;
import com.example.cartrecovery.service.EmailService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/abandoned-cart")
public class EmailController {


    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/cart/{cartId}")
    public Notification sendAbandonedCartEmail(
            @PathVariable Long cartId) {

        return emailService.sendAbandonedCartReminder(
                cartId
        );
    }

    @PostMapping("/cart/second-reminder/{cartId}")
    public Notification sendSecondAbandonedCartEmail(
            @PathVariable Long cartId) {

        return emailService.sendSecondAbandonedCartReminder(
                cartId
        );
    }

}
