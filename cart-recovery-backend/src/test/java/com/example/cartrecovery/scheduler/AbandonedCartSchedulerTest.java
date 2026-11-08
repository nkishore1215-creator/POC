package com.example.cartrecovery.scheduler;

import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartStatus;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.kafka.AbandonedCartEvent;
import com.example.cartrecovery.kafka.KafkaCartEventProducer;
import com.example.cartrecovery.repository.CartItemRepository;
import com.example.cartrecovery.repository.CartRepository;
import com.example.cartrecovery.service.AbandonedCartService;
import com.example.cartrecovery.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbandonedCartSchedulerTest {

    private static final Duration SECOND_DELAY = Duration.ofMinutes(5);
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration FIRST_DELAY = Duration.ofMinutes(5);
    private static final Duration EXPIRATION_DELAY = Duration.ofMinutes(5);

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private AbandonedCartService abandonedCartService;

    @Mock
    private EmailService emailService;

    @Mock
    private KafkaCartEventProducer producer;

    private AbandonedCartScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AbandonedCartScheduler(
                cartRepository,
                cartItemRepository,
                abandonedCartService,
                emailService,
                producer,
                IDLE_TIMEOUT,
                FIRST_DELAY,
                SECOND_DELAY
        );
    }

    @Test
    void detectsNonEmptyCartAfterThreeMinutesOfInactivity() {
        Cart cart = new Cart();
        cart.setCartId(10L);
        Customer customer = customer();
        cart.setCustomer(customer);
        cart.setTotalAmount(new BigDecimal("49.99"));
        cart.setCartStatus(CartStatus.ACTIVE);
        cart.setUpdatedAt(LocalDateTime.now().minusMinutes(4));
        cart.setReminderSent(false);

        AbandonedCart abandonedCart = abandonedCart(10L);
        abandonedCart.setReminderSent(false);

        when(cartRepository.findIdleCarts(
                eq(CartStatus.ACTIVE),
                any(LocalDateTime.class)
        )).thenReturn(List.of(cart));
        when(cartItemRepository.countByCartCartId(10L)).thenReturn(1L);
        when(cartRepository.findByCartStatusAndReminderSentFalse(
                CartStatus.ABANDONED
        )).thenReturn(List.of(cart));
        when(abandonedCartService.createForCart(10L))
                .thenReturn(abandonedCart);
        when(abandonedCartService.getDueFirstReminders(FIRST_DELAY))
                .thenReturn(List.of());

        scheduler.checkForAbandonedCarts();

        assertEquals(CartStatus.ABANDONED, cart.getCartStatus());
        assertNotNull(cart.getAbandonedAt());
        verify(cartRepository).save(cart);
        verify(abandonedCartService).createForCart(10L);
        ArgumentCaptor<AbandonedCartEvent> eventCaptor =
                ArgumentCaptor.forClass(AbandonedCartEvent.class);
        verify(producer).sendAbandonedCartEvent(eventCaptor.capture());
        assertEvent(eventCaptor.getValue(), cart, customer, BigDecimal.ZERO, "CART_ABANDONED");
        verify(emailService, never()).sendAbandonedCartReminder(10L);
    }

    @Test
    void doesNotAbandonAnEmptyIdleCart() {
        Cart cart = new Cart();
        cart.setCartId(20L);
        cart.setCartStatus(CartStatus.ACTIVE);
        cart.setUpdatedAt(LocalDateTime.now().minusMinutes(4));

        when(cartRepository.findIdleCarts(
                eq(CartStatus.ACTIVE),
                any(LocalDateTime.class)
        )).thenReturn(List.of(cart));
        when(cartItemRepository.countByCartCartId(20L)).thenReturn(0L);
        when(cartRepository.findByCartStatusAndReminderSentFalse(
                CartStatus.ABANDONED
        )).thenReturn(List.of());
        when(abandonedCartService.getDueFirstReminders(FIRST_DELAY))
                .thenReturn(List.of());

        scheduler.checkForAbandonedCarts();

        assertEquals(CartStatus.ACTIVE, cart.getCartStatus());
        verify(cartRepository, never()).save(cart);
        verifyNoInteractions(producer);
        verify(emailService, never()).sendAbandonedCartReminder(20L);
    }

    @Test
    void publishesFirstReminderAfterConfiguredAbandonmentDelay() {
        Cart cart = new Cart();
        cart.setCartId(30L);
        cart.setCartStatus(CartStatus.ABANDONED);
        cart.setReminderSent(false);

        Customer customer = customer();
        cart.setCustomer(customer);
        cart.setTotalAmount(new BigDecimal("49.99"));

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setCustomer(customer);
        abandonedCart.setDiscountAmount(new BigDecimal("5.00"));
        abandonedCart.setReminderSent(false);

        when(cartRepository.findIdleCarts(
                eq(CartStatus.ACTIVE),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(cartRepository.findByCartStatusAndReminderSentFalse(
                CartStatus.ABANDONED
        )).thenReturn(List.of(cart));
        when(abandonedCartService.createForCart(30L))
                .thenReturn(abandonedCart);
        when(abandonedCartService.getDueFirstReminders(FIRST_DELAY))
                .thenReturn(List.of(abandonedCart));

        scheduler.checkForAbandonedCarts();

        ArgumentCaptor<AbandonedCartEvent> eventCaptor =
                ArgumentCaptor.forClass(AbandonedCartEvent.class);
        verify(producer).sendAbandonedCartEvent(eventCaptor.capture());
        assertEvent(eventCaptor.getValue(), cart, customer,
                new BigDecimal("5.00"), "FIRST_REMINDER");
        verify(abandonedCartService, never()).markReminderSent(30L);
        verify(emailService, never()).sendAbandonedCartReminder(30L);
    }

    @Test
    void publishesSecondReminderForEveryDueCart() {
        AbandonedCart abandonedCart = abandonedCart(11L);
        Customer customer = customer();
        abandonedCart.setCustomer(customer);
        abandonedCart.getCart().setCustomer(customer);
        abandonedCart.getCart().setTotalAmount(new BigDecimal("49.99"));
        when(abandonedCartService.getDueSecondReminders(SECOND_DELAY))
                .thenReturn(List.of(abandonedCart));

        scheduler.sendDueSecondReminders();

        ArgumentCaptor<AbandonedCartEvent> eventCaptor =
                ArgumentCaptor.forClass(AbandonedCartEvent.class);
        verify(producer).sendAbandonedCartEvent(eventCaptor.capture());
        assertEvent(eventCaptor.getValue(), abandonedCart.getCart(), customer,
                BigDecimal.ZERO, "SECOND_REMINDER");
        verifyNoInteractions(emailService);
        verify(abandonedCartService, never()).incrementReminderCount(11L);
    }

    @Test
    void sendsConfirmationForEveryRecoveredCart() {
        AbandonedCart abandonedCart = abandonedCart(13L);
        when(abandonedCartService.getPendingRecoveryConfirmations())
                .thenReturn(List.of(abandonedCart));

        scheduler.sendRecoveryConfirmations();

        verify(emailService).sendRecoveryConfirmation(13L);
    }

    @Disabled("Expiration scheduling is currently disabled in production")
    @Test
    void expiresEveryCartFiveMinutesAfterSecondReminder() {
        AbandonedCart abandonedCart = abandonedCart(12L);
        when(abandonedCartService.getDueExpirations(EXPIRATION_DELAY))
                .thenReturn(List.of(abandonedCart));

      /*  scheduler.expireDueCarts();*/

        verify(abandonedCartService).markExpired(12L);
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setCustomerId(4L);
        customer.setFirstName("Alex");
        customer.setEmail("alex@example.com");
        customer.setPhone("+12125551234");
        return customer;
    }

    private void assertEvent(AbandonedCartEvent event, Cart cart, Customer customer,
                             BigDecimal discountAmount, String eventType) {
        assertEquals(cart.getCartId(), event.getCartId());
        assertEquals(customer.getCustomerId(), event.getCustomerId());
        assertEquals(customer.getFirstName(), event.getCustomerName());
        assertEquals(customer.getEmail(), event.getCustomerEmail());
        assertEquals(customer.getPhone(), event.getCustomerPhone());
        assertEquals(cart.getTotalAmount(), event.getTotalAmount());
        assertEquals(discountAmount, event.getDiscountAmount());
        assertEquals(eventType, event.getEventType());
    }

    private AbandonedCart abandonedCart(Long cartId) {
        Cart cart = new Cart();
        cart.setCartId(cartId);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        return abandonedCart;
    }
}
