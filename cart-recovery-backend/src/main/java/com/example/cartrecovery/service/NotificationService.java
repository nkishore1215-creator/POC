package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartStatus;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.entity.Notification;
import com.example.cartrecovery.entity.NotificationChannel;
import com.example.cartrecovery.exception.AbandonedCartNotFoundException;
import com.example.cartrecovery.exception.CartNotFoundException;
import com.example.cartrecovery.repository.AbandonedCartRepository;
import com.example.cartrecovery.repository.CartRepository;
import com.example.cartrecovery.repository.CustomerRepository;
import com.example.cartrecovery.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;
    private final AbandonedCartRepository abandonedCartRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            CartRepository cartRepository,
            CustomerRepository customerRepository,
            AbandonedCartRepository abandonedCartRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.cartRepository = cartRepository;
        this.customerRepository = customerRepository;
        this.abandonedCartRepository = abandonedCartRepository;
    }

    public Notification createNotificationForCart(Long cartId, String type) {
        return createNotificationForCart(
                cartId,
                type,
                NotificationChannel.EMAIL
        );
    }

    public Notification createNotificationForCart(
            Long cartId,
            String type,
            NotificationChannel channel) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        Customer customer = cart.getCustomer();

        Notification notification = new Notification();
        notification.setCart(cart);
        notification.setCustomer(customer);
        notification.setNotificationChannel(channel);
        notification.setNotificationType(type);
        notification.setNotificationStatus("PENDING");
        notification.setSentAt(null);

        return notificationRepository.save(notification);
    }

    /**
     * Persists all state produced by a successfully delivered reminder in one
     * transaction so the notification, abandoned cart, and cart cannot diverge.
     */
    public Notification recordSentReminder(
            Long cartId,
            String notificationType,
            NotificationChannel channel,
            String messageBody) {
        Notification notification = createNotificationForCart(
                cartId,
                notificationType,
                channel



        );

        AbandonedCart abandonedCart = abandonedCartRepository
                .findByCartCartId(cartId)
                .orElseThrow(() -> new AbandonedCartNotFoundException(cartId));

        LocalDateTime sentAt = LocalDateTime.now();
        int currentReminderCount = abandonedCart.getReminderCount() == null
                ? 0
                : abandonedCart.getReminderCount();

        abandonedCart.setReminderCount(currentReminderCount + 1);
        abandonedCart.setReminderSent(true);
        abandonedCart.setReminderSentAt(sentAt);
        abandonedCart.setRecoveryStatus("REMINDER_SENT");
        abandonedCart.setNotificationChannel(channel);

        Cart cart = abandonedCart.getCart();
        if (cart == null) {
            cart = notification.getCart();
        }
        if (cart == null) {
            throw new IllegalStateException(
                    "Cart is not available for reminder notification: " + cartId
            );
        }

        cart.setReminderSent(true);
        cart.setCartStatus(CartStatus.REMINDER_SENT);
        cart.setUpdatedAt(sentAt);

        notification.setNotificationStatus("SENT");
        notification.setSentAt(sentAt);
        notification.setMessage(messageBody);


        cartRepository.save(cart);
        abandonedCartRepository.save(abandonedCart);
        return notificationRepository.save(notification);
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));
    }

    public List<Notification> getNotificationsByCart(Long cartId) {
        return notificationRepository.findByCartCartId(cartId);
    }

    public List<Notification> getNotificationsByCustomer(Long customerId) {
        return notificationRepository.findByCustomerCustomerId(customerId);
    }

    public List<Notification> getPendingNotifications() {
        return notificationRepository.findByNotificationStatus("PENDING");
    }

    public boolean hasSentNotification(Long cartId, String notificationType) {
        return notificationRepository
                .existsByCartCartIdAndNotificationTypeAndNotificationStatus(
                        cartId,
                        notificationType,
                        "SENT"
                );
    }

    /**
     * Mark as sent locally (does not actually send email/SMS)
     */
    public Notification markSent(Long notificationId) {
        Notification n = getNotificationById(notificationId);

        if (!"SENT".equalsIgnoreCase(n.getNotificationStatus())
                && isReminder(n)) {
            Cart cart = n.getCart();
            if (cart == null || cart.getCartId() == null) {
                throw new IllegalStateException(
                        "Cart is not available for reminder notification ID: "
                                + notificationId
                );
            }

            AbandonedCart abandonedCart = abandonedCartRepository
                    .findByCartCartId(cart.getCartId())
                    .orElseThrow(() ->
                            new AbandonedCartNotFoundException(cart.getCartId())
                    );

            int currentReminderCount =
                    abandonedCart.getReminderCount() == null
                    ? 0
                    : abandonedCart.getReminderCount();
            abandonedCart.setReminderCount(currentReminderCount + 1);
            abandonedCartRepository.save(abandonedCart);
        }

        n.setNotificationStatus("SENT");
        n.setSentAt(LocalDateTime.now());
        return notificationRepository.save(n);
    }

    private boolean isReminder(Notification notification) {
        String type = notification.getNotificationType();
        return type != null && type.startsWith("EMAIL_REMINDER_");
    }

    /**
     * Mark as failed
     */
    public Notification markFailed(Long notificationId) {
        Notification n = getNotificationById(notificationId);
        n.setNotificationStatus("FAILED");
        n.setSentAt(LocalDateTime.now());
        return notificationRepository.save(n);
    }

    /**
     * Mark a FAILED notification back to PENDING so it can be retried.
     */
    public Notification retryNotification(Long notificationId) {
        Notification n = getNotificationById(notificationId);
        if (!"FAILED".equalsIgnoreCase(n.getNotificationStatus())) {
            throw new IllegalStateException("Only FAILED notifications can be retried.");
        }
        n.setNotificationStatus("PENDING");
        n.setSentAt(null);
        return notificationRepository.save(n);
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
