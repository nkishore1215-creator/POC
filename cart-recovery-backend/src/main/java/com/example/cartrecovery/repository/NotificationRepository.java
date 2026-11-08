package com.example.cartrecovery.repository;

import com.example.cartrecovery.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // Find all notifications for a specific cart
    List<Notification> findByCartCartId(Long cartId);

    // Find all notifications for a specific customer
    List<Notification> findByCustomerCustomerId(Long customerId);

    // Find notifications by type (EMAIL, SMS)
    List<Notification> findByNotificationType(String notificationType);

    // Find notifications by status (PENDING, SENT, FAILED)
    List<Notification> findByNotificationStatus(String notificationStatus);

    // Find notifications by type and status
    List<Notification> findByNotificationTypeAndNotificationStatus(
            String notificationType,
            String notificationStatus
    );

    // Find notifications for a cart by status
    List<Notification> findByCartCartIdAndNotificationStatus(
            Long cartId,
            String notificationStatus
    );

    boolean existsByCartCartIdAndNotificationTypeAndNotificationStatus(
            Long cartId,
            String notificationType,
            String notificationStatus
    );

    boolean existsByCartCartIdAndNotificationTypeAndNotificationStatusAndSentAtGreaterThanEqual(
            Long cartId,
            String notificationType,
            String notificationStatus,
            LocalDateTime sentAt
    );

}
