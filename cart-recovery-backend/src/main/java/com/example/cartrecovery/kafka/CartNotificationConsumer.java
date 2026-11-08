package com.example.cartrecovery.kafka;

import com.example.cartrecovery.entity.CartItem;
import com.example.cartrecovery.entity.NotificationChannel;
import com.example.cartrecovery.entity.Product;
import com.example.cartrecovery.repository.CartItemRepository;
import com.example.cartrecovery.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.Objects;
import com.example.cartrecovery.util.DiscountCodeGenerator;
import java.util.stream.Collectors;

@Service
public class CartNotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(CartNotificationConsumer.class);
    private static final String FIRST_REMINDER_TYPE = "EMAIL_REMINDER_1";
    private static final String SECOND_REMINDER_TYPE = "EMAIL_REMINDER_2";

    private final EmailService emailService;
    private final AbandonedCartService abandonedCartService;
    private final NotificationService notificationService;
    private final CartItemRepository cartItemRepository;
    private final RecoveryDiscountPolicy recoveryDiscountPolicy;

    public CartNotificationConsumer(
            EmailService emailService,
            AbandonedCartService abandonedCartService,
            NotificationService notificationService,
            CartItemRepository cartItemRepository,
            RecoveryDiscountPolicy recoveryDiscountPolicy) {
        this.emailService = emailService;
        this.abandonedCartService = abandonedCartService;
        this.notificationService = notificationService;
        this.cartItemRepository=cartItemRepository;
        this.recoveryDiscountPolicy=recoveryDiscountPolicy;
    }


    @KafkaListener(
            topics = "abandoned-cart-events",
            groupId = "cart-recovery-notification-group"
    )
    public void consume(AbandonedCartEvent event) {
        try {
            switch (event.getEventType()) {
                case "CART_ABANDONED" ->
                        log.info("Cart abandoned event received for cartId={}", event.getCartId());
                case "FIRST_REMINDER" -> {
                    sendReminder(event, "First Reminder - you left items in your cart", FIRST_REMINDER_TYPE);
                    abandonedCartService.markReminderSent(event.getCartId());
                    log.info("First reminder sent for cartId={}", event.getCartId());
                }
                case "SECOND_REMINDER" -> {
                    sendReminder(event, "Second reminder - your cart", SECOND_REMINDER_TYPE);
                    abandonedCartService.markSecondReminderSent(event.getCartId());
                    log.info("Second reminder sent for cartId={}", event.getCartId());
                }
                default -> log.warn("Unknown Kafka event type={}", event.getEventType());
            }
        } catch (Exception exception) {
            log.error("Failed to process Kafka event cartId={}, type={}",
                    event.getCartId(),
                    event.getEventType(),
                    exception);
        }
    }


    private void sendReminder(AbandonedCartEvent event, String subject, String notificationType) {
        String cartUrl="http://localhost:5173/cart/"+event.getCartId();

        BigDecimal cartTotal=event.getTotalAmount();
        int discountPercentage = SECOND_REMINDER_TYPE.equals(notificationType)
                ? recoveryDiscountPolicy.percentageFor(cartTotal)
                : 0;

        String discountCode = discountPercentage > 0
                ? DiscountCodeGenerator.generate("RECOVER")
                : null;


        String productNames = cartItemRepository.findByCartCartId(event.getCartId()).stream()
                .map(CartItem::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getProductName)
                .filter(Objects::nonNull)
                .map(HtmlUtils::htmlEscape)
                .map(name -> "<li><b>" + name + "</b></li>")
                .collect(Collectors.joining());
        String emailBody = "Hi <b>" + event.getCustomerName() + "</b>, <br><br>" +

                "you have left your items in your cart please have a look at the items &purchase"+"<br><br>"+

                "Your cart ID : <b>"  + event.getCartId() + "</b><br><br>" +

                "Product(s) : <ul>" + productNames + "</ul><br>"+
                "Cart Total : <b>$" + cartTotal + "</b><br><br>";



        if (discountCode!=null){

            emailBody+=
            "Special offer for you: <b>"+discountPercentage +"% OFF </b><br><br>"+

                    "Use Discount Code: <b>"+discountCode+ "</b><br><br>";

        }



   emailBody+=
               "<a href="+cartUrl+"/>Please complete purchase.</a>";


        emailService.sendEmail(event.getCustomerEmail(), subject, emailBody);
        if (discountCode != null) {
            abandonedCartService.storeDiscountCode(
                    event.getCartId(), discountCode, discountPercentage);
        }
        notificationService.recordSentReminder(
                event.getCartId(),
                notificationType,
                NotificationChannel.EMAIL,
                emailBody);
    }
}
