package com.example.cartrecovery.kafka;

import com.example.cartrecovery.entity.CartItem;
import com.example.cartrecovery.entity.Product;

import com.example.cartrecovery.repository.CartItemRepository;
import com.example.cartrecovery.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CartNotificationConsumerTest {
    @Mock private EmailService emailService;
    @Mock private AbandonedCartService abandonedCartService;
    @Mock private NotificationService notificationService;
    @Mock private CartItemRepository cartItemRepository;
    private CartNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CartNotificationConsumer(emailService, abandonedCartService,
                notificationService, cartItemRepository, new RecoveryDiscountPolicy(
                new BigDecimal("500"), new BigDecimal("1000"), new BigDecimal("2000")));
    }

    @ParameterizedTest
    @CsvSource({"500,2", "999.99,2", "1000,5", "1999.99,5", "2000,10"})
    void storesEmailedCodeOnlyAfterSuccessfulSecondEmail(String total, int percentage) {
        consumer.consume(event("SECOND_REMINDER", total));

        var order = inOrder(emailService, abandonedCartService);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        order.verify(emailService).sendEmail(eq("alex@example.com"), anyString(),
                body.capture());
        order.verify(abandonedCartService).storeDiscountCode(eq(30L), code.capture(), eq(percentage));
        assertTrue(code.getValue().matches("RECOVER-[0-9]{10}"));
        assertTrue(body.getValue().contains("Use Discount Code: <b>" + code.getValue() + "</b>"));
        assertTrue(body.getValue().contains(percentage + "% OFF"));
        order.verify(abandonedCartService).markSecondReminderSent(30L);
    }

    @Test
    void includesEscapedProductNamesAsSeparateListItemsInEmail() {
        Product keyboard = new Product();
        keyboard.setProductName("Keyboard & Mouse");
        CartItem firstItem = new CartItem();
        firstItem.setProduct(keyboard);
        Product monitor = new Product();
        monitor.setProductName("Monitor");
        CartItem secondItem = new CartItem();
        secondItem.setProduct(monitor);
        when(cartItemRepository.findByCartCartId(30L)).thenReturn(List.of(firstItem, secondItem));

        consumer.consume(event("FIRST_REMINDER", "1000"));

        verify(emailService).sendEmail(eq("alex@example.com"), anyString(),
                contains("<ul><li><b>Keyboard &amp; Mouse</b></li>"
                        + "<li><b>Monitor</b></li></ul>"));
    }

    @Test
    void doesNotStoreCodeWhenEmailFails() {
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        consumer.consume(event("SECOND_REMINDER", "2000"));

        verifyNoInteractions(abandonedCartService, notificationService);
    }

    @ParameterizedTest
    @CsvSource({"FIRST_REMINDER,2000", "SECOND_REMINDER,499.99"})
    void doesNotOfferOrStoreDiscountOutsideEligibleSecondReminder(String type, String total) {
        consumer.consume(event(type, total));

        verify(emailService).sendEmail(anyString(), anyString(),
                argThat(body -> !body.contains("Discount Code")));
        verify(abandonedCartService, never()).storeDiscountCode(anyLong(), anyString(), anyInt());
    }

    private AbandonedCartEvent event(String type, String total) {
        return new AbandonedCartEvent(30L, 4L, "Alex", "+12125551234",
                "alex@example.com", new BigDecimal(total), BigDecimal.ZERO, type);
    }
}
