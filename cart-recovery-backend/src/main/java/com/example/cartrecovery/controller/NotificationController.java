package com.example.cartrecovery.controller;

import com.example.cartrecovery.entity.Notification;
import com.example.cartrecovery.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/cart/{cartId}")
    public List<Notification> getNotificationsByCart(
            @PathVariable Long cartId) {

        return notificationService.getNotificationsByCart(cartId);
    }
}