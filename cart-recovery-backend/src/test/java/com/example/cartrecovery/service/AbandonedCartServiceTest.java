package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartStatus;
import com.example.cartrecovery.repository.AbandonedCartRepository;
import com.example.cartrecovery.repository.CartRepository;
import com.example.cartrecovery.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbandonedCartServiceTest {

    @Mock
    private AbandonedCartRepository abandonedCartRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AbandonedCartService abandonedCartService;

    @Test
    void storesExactEmailedDiscountCodeAndPercentage() {
        AbandonedCart cart = new AbandonedCart();
        when(abandonedCartRepository.findByCartCartId(30L))
                .thenReturn(java.util.Optional.of(cart));
        when(abandonedCartRepository.save(cart)).thenReturn(cart);

        AbandonedCart saved = abandonedCartService.storeDiscountCode(30L, "SAVE5", 5);

        assertEquals("SAVE5", saved.getDiscountCode());
        assertEquals(5, saved.getDiscountPercentage());
        verify(abandonedCartRepository).save(cart);
    }

    @Test
    void returnsFirstReminderAfterAbandonmentDelayHasElapsed() {
        Cart cart = new Cart();
        cart.setCartId(30L);
        cart.setCartStatus(CartStatus.ABANDONED);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setReminderSent(false);
        abandonedCart.setRecoveryStatus("PENDING");
        abandonedCart.setAbandonedAt(LocalDateTime.now().minusMinutes(4));

        when(abandonedCartRepository
                .findByReminderSentFalseAndAbandonedAtLessThanEqualAndRecoveryStatusNot(
                        any(LocalDateTime.class),
                        eq("RECOVERED")
                ))
                .thenReturn(List.of(abandonedCart));

        List<AbandonedCart> dueReminders =
                abandonedCartService.getDueFirstReminders(
                        Duration.ofMinutes(3)
                );

        assertEquals(List.of(abandonedCart), dueReminders);
    }

    @Test
    void marksAbandonedCartRecoveredWithoutClaimingUnsentReminder() {
        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setReminderSent(false);

        when(abandonedCartRepository.findByCartCartId(2L))
                .thenReturn(java.util.Optional.of(abandonedCart));
        when(abandonedCartRepository.save(abandonedCart))
                .thenReturn(abandonedCart);

        AbandonedCart recovered =
                abandonedCartService.markRecovered(2L);

        assertEquals("RECOVERED", recovered.getRecoveryStatus());
        assertTrue(recovered.getRecovered());
        assertNotNull(recovered.getRecoveredAt());
        assertEquals(recovered.getRecoveredAt(), recovered.getRecoveredTime());
        assertFalse(recovered.getReminderSent());
        verify(abandonedCartRepository).save(abandonedCart);
    }

    @Test
    void returnsCartFiveMinutesAfterSuccessfulSecondReminder() {
        Cart cart = new Cart();
        cart.setCartId(3L);
        cart.setCartStatus(CartStatus.REMINDER_SENT);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setRecoveryStatus("REMINDER_SENT");

        when(abandonedCartRepository
                .findDueAfterSuccessfulNotification(
                        any(LocalDateTime.class),
                        eq("EMAIL_REMINDER_2"),
                        eq("SENT"),
                        eq(CartStatus.REMINDER_SENT),
                        eq("RECOVERED")
                ))
                .thenReturn(List.of(abandonedCart));

        List<AbandonedCart> due =
                abandonedCartService.getDueExpirations(
                        Duration.ofMinutes(5)
                );

        assertEquals(List.of(abandonedCart), due);
    }

    @Test
    void returnsSecondReminderWhenOnlyAnOlderCycleSentOne() {
        LocalDateTime firstReminderSentAt =
                LocalDateTime.now().minusMinutes(6);

        Cart cart = new Cart();
        cart.setCartId(31L);
        cart.setCartStatus(CartStatus.REMINDER_SENT);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setReminderSent(true);
        abandonedCart.setReminderSentAt(firstReminderSentAt);
        abandonedCart.setRecoveryStatus("REMINDER_SENT");

        when(abandonedCartRepository
                .findByReminderSentTrueAndReminderSentAtLessThanEqualAndRecoveryStatusNot(
                        any(LocalDateTime.class),
                        eq("RECOVERED")
                ))
                .thenReturn(List.of(abandonedCart));
        when(notificationRepository
                .existsByCartCartIdAndNotificationTypeAndNotificationStatusAndSentAtGreaterThanEqual(
                        31L,
                        "EMAIL_REMINDER_2",
                        "SENT",
                        firstReminderSentAt
                ))
                .thenReturn(false);

        List<AbandonedCart> due =
                abandonedCartService.getDueSecondReminders(
                        Duration.ofMinutes(5)
                );

        assertEquals(List.of(abandonedCart), due);
    }

    @Test
    void excludesSecondReminderAlreadySentInCurrentCycle() {
        LocalDateTime firstReminderSentAt =
                LocalDateTime.now().minusMinutes(6);

        Cart cart = new Cart();
        cart.setCartId(32L);
        cart.setCartStatus(CartStatus.REMINDER_SENT);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setReminderSent(true);
        abandonedCart.setReminderSentAt(firstReminderSentAt);
        abandonedCart.setRecoveryStatus("REMINDER_SENT");

        when(abandonedCartRepository
                .findByReminderSentTrueAndReminderSentAtLessThanEqualAndRecoveryStatusNot(
                        any(LocalDateTime.class),
                        eq("RECOVERED")
                ))
                .thenReturn(List.of(abandonedCart));
        when(notificationRepository
                .existsByCartCartIdAndNotificationTypeAndNotificationStatusAndSentAtGreaterThanEqual(
                        32L,
                        "EMAIL_REMINDER_2",
                        "SENT",
                        firstReminderSentAt
                ))
                .thenReturn(true);

        List<AbandonedCart> due =
                abandonedCartService.getDueSecondReminders(
                        Duration.ofMinutes(5)
                );

        assertTrue(due.isEmpty());
    }

    @Test
    void marksDueCartExpiredInBothTables() {
        Cart cart = new Cart();
        cart.setCartId(4L);
        cart.setCartStatus(CartStatus.REMINDER_SENT);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setRecoveryStatus("REMINDER_SENT");

        when(abandonedCartRepository.findByCartCartId(4L))
                .thenReturn(java.util.Optional.of(abandonedCart));
        when(abandonedCartRepository.save(abandonedCart))
                .thenReturn(abandonedCart);

        AbandonedCart expired =
                abandonedCartService.markExpired(4L);

        assertEquals(CartStatus.EXPIRED, cart.getCartStatus());
        assertEquals("EXPIRED", expired.getRecoveryStatus());
        assertNotNull(cart.getUpdatedAt());
        verify(cartRepository).save(cart);
        verify(abandonedCartRepository).save(abandonedCart);
    }

    @Test
    void resetsRecoveryToPendingAfterCustomerReturns() {
        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setRecoveryStatus("REMINDER_SENT");
        abandonedCart.setReminderSent(true);
        abandonedCart.setReminderSentAt(LocalDateTime.now());
        abandonedCart.setReminderCount(1);
        abandonedCart.setDiscountCode("RECOVER-5-OLD");
        abandonedCart.setDiscountPercentage(5);

        when(abandonedCartRepository.findByCartCartId(5L))
                .thenReturn(java.util.Optional.of(abandonedCart));

        abandonedCartService.resetForCustomerActivity(5L);

        assertEquals("PENDING", abandonedCart.getRecoveryStatus());
        assertFalse(abandonedCart.getReminderSent());
        assertEquals(null, abandonedCart.getReminderSentAt());
        assertEquals(0, abandonedCart.getReminderCount());
        assertEquals(null, abandonedCart.getDiscountCode());
        assertEquals(null, abandonedCart.getDiscountPercentage());
        verify(abandonedCartRepository).save(abandonedCart);
    }

    @Test
    void refreshesAbandonedTimeForARepeatedAbandonmentCycle() {
        LocalDateTime newAbandonedAt =
                LocalDateTime.now().minusMinutes(1);

        Cart cart = new Cart();
        cart.setCartId(6L);
        cart.setCartStatus(CartStatus.ABANDONED);
        cart.setAbandonedAt(newAbandonedAt);

        AbandonedCart existing = new AbandonedCart();
        existing.setCart(cart);
        existing.setAbandonedAt(
                LocalDateTime.now().minusHours(1)
        );
        existing.setRecoveryStatus("PENDING");

        when(cartRepository.findById(6L))
                .thenReturn(java.util.Optional.of(cart));
        when(abandonedCartRepository.findByCartCartId(6L))
                .thenReturn(java.util.Optional.of(existing));
        when(abandonedCartRepository.save(existing))
                .thenReturn(existing);

        AbandonedCart refreshed =
                abandonedCartService.createForCart(6L);

        assertEquals(newAbandonedAt, refreshed.getAbandonedAt());
        assertEquals("PENDING", refreshed.getRecoveryStatus());
        assertFalse(refreshed.getReminderSent());
    }

    @Test
    void returnsRecoveredCartWithoutSentConfirmation() {
        Cart cart = new Cart();
        cart.setCartId(7L);
        cart.setCartStatus(CartStatus.RECOVERED);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setRecoveryStatus("RECOVERED");

        when(abandonedCartRepository.findByRecoveryStatus("RECOVERED"))
                .thenReturn(List.of(abandonedCart));
        when(notificationRepository
                .existsByCartCartIdAndNotificationTypeAndNotificationStatus(
                        7L,
                        "EMAIL_RECOVERY_CONFIRMATION",
                        "SENT"
                ))
                .thenReturn(false);

        List<AbandonedCart> pending =
                abandonedCartService.getPendingRecoveryConfirmations();

        assertEquals(List.of(abandonedCart), pending);
    }
}
