package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartStatus;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.entity.Notification;
import com.example.cartrecovery.repository.CartItemRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AbandonedCartService abandonedCartService;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private RecoveryMessageService recoveryMessageService;

    @Mock
    private RecoveryDiscountPolicy recoveryDiscountPolicy;

    @InjectMocks
    private EmailService emailService;

    private AbandonedCart abandonedCart;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                emailService,
                "senderEmail",
                "sender@example.com"
        );
        ReflectionTestUtils.setField(
                emailService,
                "senderPassword",
                "test-app-password"
        );
        ReflectionTestUtils.setField(
                emailService,
                "cartPageUrl",
                "http://localhost:3000/cart"
        );
        ReflectionTestUtils.setField(
                emailService,
                "discountCodePrefix",
                "RECOVER"
        );
        ReflectionTestUtils.setField(
                emailService,
                "firstReminderOfferEnabled",
                false
        );
        ReflectionTestUtils.setField(
                emailService,
                "accessoryOfferMessage",
                "Complete your setup and explore special offers on other accessories."
        );
        org.mockito.Mockito.lenient()
                .when(javaMailSender.createMimeMessage()).thenAnswer(invocation ->
                new MimeMessage(Session.getInstance(new Properties()))
        );

        Customer customer = new Customer();
        customer.setFirstName("Alex");
        customer.setEmail("alex@example.com");
        customer.setEmailOptIn(true);

        Cart cart = new Cart();
        cart.setCartId(7L);
        cart.setCustomer(customer);
        cart.setCartStatus(CartStatus.REMINDER_SENT);
        cart.setTotalAmount(new BigDecimal("75.00"));

        abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setCustomer(customer);
        abandonedCart.setReminderSent(true);
        abandonedCart.setRecoveryStatus("REMINDER_SENT");
        abandonedCart.setDiscountCode("RECOVER-7-TEST");
    }

    @Test
    void sendsFirstReminderWithoutDiscountCode() throws Exception {
        abandonedCart.setReminderSent(false);
        abandonedCart.setRecoveryStatus("PENDING");
        abandonedCart.setDiscountCode(null);
        abandonedCart.getCart().setCartStatus(CartStatus.ABANDONED);

        Notification pending = notification(100L);
        Notification sent = notification(100L);

        when(abandonedCartService.getByCartId(7L))
                .thenReturn(abandonedCart);
        when(notificationService.createNotificationForCart(
                7L,
                "EMAIL_REMINDER_1"
        )).thenReturn(pending);
        when(notificationService.markSent(100L)).thenReturn(sent);
        stubRecoveryMessage(
                RecoveryMessageService.ReminderStage.FIRST,
                "You left something in your cart"
        );

        Notification result = emailService.sendAbandonedCartReminder(7L);

        assertEquals(sent, result);
        verify(notificationService).createNotificationForCart(
                7L,
                "EMAIL_REMINDER_1"
        );
        verify(abandonedCartService, never()).ensureDiscountCode(
                any(Long.class),
                any(String.class),
                anyInt()
        );
        verify(abandonedCartService).markReminderSent(7L);
        assertEmailContains(
                "You left something in your cart",
                "explore special offers on other accessories",
                "12 customer(s) in the last month, 25 unit(s) sold"
        );
        assertEmailDoesNotContain("discount code", "discountCode=");
    }

    @Test
    void rejectsMissingSmtpConfigurationBeforeCreatingNotification() {
        abandonedCart.setReminderSent(false);
        abandonedCart.setRecoveryStatus("PENDING");
        abandonedCart.getCart().setCartStatus(CartStatus.ABANDONED);
        ReflectionTestUtils.setField(emailService, "senderPassword", "");

        when(abandonedCartService.getByCartId(7L))
                .thenReturn(abandonedCart);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> emailService.sendAbandonedCartReminder(7L)
        );

        assertTrue(exception.getMessage().contains("MAIL_APP_PASSWORD"));
        verify(notificationService, never()).createNotificationForCart(
                any(Long.class),
                any(String.class)
        );
    }

    @Test
    void sendsSecondReminderOnceFirstReminderHasBeenSent() throws Exception {
        Notification pending = notification(101L);
        Notification sent = notification(101L);
        abandonedCart.getCart().setTotalAmount(new BigDecimal("2000.00"));
        abandonedCart.setDiscountPercentage(10);

        when(abandonedCartService.getByCartId(7L))
                .thenReturn(abandonedCart);
        when(abandonedCartService
                .hasSecondReminderBeenSentInCurrentCycle(7L))
                .thenReturn(false);
        when(recoveryDiscountPolicy.percentageFor(new BigDecimal("2000.00")))
                .thenReturn(10);
        when(abandonedCartService.ensureDiscountCode(7L, "RECOVER", 10))
                .thenReturn(abandonedCart);
        when(notificationService.createNotificationForCart(
                7L,
                "EMAIL_REMINDER_2"
        )).thenReturn(pending);
        when(notificationService.markSent(101L)).thenReturn(sent);
        stubRecoveryMessage(
                RecoveryMessageService.ReminderStage.SECOND,
                "Second reminder: your cart is waiting"
        );

        Notification result =
                emailService.sendSecondAbandonedCartReminder(7L);

        assertEquals(sent, result);
        assertEmailContains(
                "Second reminder: your cart is waiting",
                "<b>Your discount:</b> 10% off",
                "RECOVER-7-TEST"
        );
        assertEmailDoesNotContain("discountCode=");
    }

    @Test
    void sendsRecoveryConfirmationAfterRecoveredCheckout() throws Exception {
        abandonedCart.setRecoveryStatus("RECOVERED");
        abandonedCart.getCart().setCartStatus(CartStatus.RECOVERED);

        Notification pending = notification(102L);
        Notification sent = notification(102L);

        when(abandonedCartService.getByCartId(7L))
                .thenReturn(abandonedCart);
        when(notificationService.hasSentNotification(
                7L,
                "EMAIL_RECOVERY_CONFIRMATION"
        )).thenReturn(false);
        when(notificationService.createNotificationForCart(
                7L,
                "EMAIL_RECOVERY_CONFIRMATION"
        )).thenReturn(pending);
        when(notificationService.markSent(102L)).thenReturn(sent);

        Notification result =
                emailService.sendRecoveryConfirmation(7L);

        assertEquals(sent, result);
        assertEmailContains(
                "Thank you for completing your purchase!",
                "<p>Hi <b>Alex</b>,</p>",
                "<p>Thank you for completing your purchase.</p>",
                "<p>We're happy you came back and finished your order.</p>",
                "<p><b>Cart ID:</b> 7<br><b>Product(s) :</b>",
                "<br><b>Order total:</b> $75.00</p>",
                "<p>Thank you for shopping with us!</p>",
                "<p>Thank you,<br>Cart Recovery Team</p>"
        );
        assertEmailDoesNotContain("</br>");
    }

    private Notification notification(Long id) {
        Notification notification = new Notification();
        notification.setNotificationId(id);
        return notification;
    }

    private void stubRecoveryMessage(
            RecoveryMessageService.ReminderStage stage,
            String subject
    ) {
        when(recoveryMessageService.createMessage(
                any(Customer.class),
                any(Cart.class),
                any(AbandonedCart.class),
                org.mockito.ArgumentMatchers.eq(stage)
        )).thenReturn(new RecoveryMessage(
                subject,
                "Your cart is waiting",
                "Your selected products are still available in your cart.",
                java.util.List.of(new RecoveryMessage.ProductSummary(
                        "Developer Laptop",
                        "LAPTOP",
                        new BigDecimal("1199.00"),
                        1,
                        12,
                        25,
                        true
                ))
        ));
    }

    private void assertEmailContains(String subject, String... bodyText) throws Exception {
        ArgumentCaptor<MimeMessage> messageCaptor =
                ArgumentCaptor.forClass(MimeMessage.class);

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();
        assertEquals(subject, message.getSubject());
        String content = message.getContent().toString();
        for (String text : bodyText) {
            assertTrue(content.contains(text), () -> "Email body is missing: " + text);
        }
    }

    private void assertEmailDoesNotContain(String... bodyText) throws Exception {
        ArgumentCaptor<MimeMessage> messageCaptor =
                ArgumentCaptor.forClass(MimeMessage.class);

        verify(javaMailSender).send(messageCaptor.capture());

        String content = messageCaptor.getValue().getContent().toString();
        for (String text : bodyText) {
            assertFalse(content.contains(text));
        }
    }
}
