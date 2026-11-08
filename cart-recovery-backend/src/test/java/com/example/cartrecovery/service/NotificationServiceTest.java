package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartStatus;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.entity.Notification;
import com.example.cartrecovery.entity.NotificationChannel;
import com.example.cartrecovery.repository.AbandonedCartRepository;
import com.example.cartrecovery.repository.CartRepository;
import com.example.cartrecovery.repository.CustomerRepository;
import com.example.cartrecovery.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AbandonedCartRepository abandonedCartRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void incrementsReminderCountAfterFirstSuccessfulSend() {
        Cart cart = new Cart();
        cart.setCartId(2L);

        Notification notification = new Notification();
        notification.setNotificationId(10L);
        notification.setCart(cart);
        notification.setNotificationType("EMAIL_REMINDER_1");
        notification.setNotificationStatus("PENDING");

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setReminderCount(0);

        when(notificationRepository.findById(10L))
                .thenReturn(Optional.of(notification));
        when(abandonedCartRepository.findByCartCartId(2L))
                .thenReturn(Optional.of(abandonedCart));
        when(notificationRepository.save(notification))
                .thenReturn(notification);

        Notification result = notificationService.markSent(10L);

        assertEquals(1, abandonedCart.getReminderCount());
        assertEquals("SENT", result.getNotificationStatus());
        verify(abandonedCartRepository).save(abandonedCart);
        verify(notificationRepository).save(notification);
    }

    @Test
    void doesNotIncrementReminderCountWhenAlreadySent() {
        Notification notification = new Notification();
        notification.setNotificationId(10L);
        notification.setNotificationType("EMAIL_REMINDER_1");
        notification.setNotificationStatus("SENT");

        when(notificationRepository.findById(10L))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification))
                .thenReturn(notification);

        Notification result = notificationService.markSent(10L);

        assertEquals("SENT", result.getNotificationStatus());
        verify(abandonedCartRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordsSuccessfulRcsReminderAndUpdatesRecoveryStatus() {
        Customer customer = new Customer();
        customer.setCustomerId(4L);

        Cart cart = new Cart();
        cart.setCartId(18L);
        cart.setCustomer(customer);
        cart.setCartStatus(CartStatus.ABANDONED);
        cart.setReminderSent(false);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setCustomer(customer);
        abandonedCart.setReminderCount(0);
        abandonedCart.setReminderSent(false);
        abandonedCart.setRecoveryStatus("PENDING");

        when(cartRepository.findById(18L)).thenReturn(Optional.of(cart));
        when(abandonedCartRepository.findByCartCartId(18L))
                .thenReturn(Optional.of(abandonedCart));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.recordSentReminder(
                18L,
                "RCS_REMINDER_1",
                NotificationChannel.RCS,
                "somthing left cart"
        );

        assertEquals("SENT", result.getNotificationStatus());
        assertEquals("RCS_REMINDER_1", result.getNotificationType());
        assertEquals(NotificationChannel.RCS, result.getNotificationChannel());
        assertNotNull(result.getSentAt());

        assertEquals("REMINDER_SENT", abandonedCart.getRecoveryStatus());
        assertEquals(true, abandonedCart.getReminderSent());
        assertEquals(1, abandonedCart.getReminderCount());
        assertEquals(NotificationChannel.RCS, abandonedCart.getNotificationChannel());
        assertEquals(result.getSentAt(), abandonedCart.getReminderSentAt());

        assertEquals(CartStatus.REMINDER_SENT, cart.getCartStatus());
        assertEquals(true, cart.getReminderSent());
        assertEquals(result.getSentAt(), cart.getUpdatedAt());

        verify(cartRepository).save(cart);
        verify(abandonedCartRepository).save(abandonedCart);
    }
}
