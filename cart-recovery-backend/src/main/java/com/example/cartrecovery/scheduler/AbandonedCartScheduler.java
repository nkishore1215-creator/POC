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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AbandonedCartScheduler {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartScheduler.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AbandonedCartService abandonedCartService;
    private final EmailService emailService;
    private final Duration idleTimeout;
    private final Duration firstReminderDelay;
    private final Duration secondReminderDelay;

    private final KafkaCartEventProducer kafkaCartEventProducer;


    public AbandonedCartScheduler(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            AbandonedCartService abandonedCartService,
            EmailService emailService,
            KafkaCartEventProducer kafkaCartEventProducer,
            @Value("${abandoned.cart.idle-timeout:PT3M}")
            Duration idleTimeout,
            @Value("${abandoned.cart.first-reminder-delay:PT1M}")
            Duration firstReminderDelay,
            @Value("${abandoned.cart.second-reminder-delay:PT1M}")
            Duration secondReminderDelay
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.abandonedCartService = abandonedCartService;
        this.emailService = emailService;
        this.idleTimeout = idleTimeout;
        this.firstReminderDelay = firstReminderDelay;
        this.secondReminderDelay = secondReminderDelay;
        this.kafkaCartEventProducer=kafkaCartEventProducer;

    }

    /**
     * Detects non-empty active carts that have had no activity for the configured
     * timeout, records their abandonment, and sends reminders whose delay elapsed.
     */
    @Scheduled(
            fixedDelayString = "#{T(java.time.Duration).parse('${abandoned.cart.scan-interval:PT30S}').toMillis()}",
            initialDelayString = "#{T(java.time.Duration).parse('${abandoned.cart.initial-delay:PT5S}').toMillis()}"
    )
    public void checkForAbandonedCarts() {

        LocalDateTime cutoff = LocalDateTime.now().minus(idleTimeout);
        log.debug(
                "Scanning for active carts idle longer than {} (cutoff={})",
                idleTimeout,
                cutoff
        );

        List<Cart> idleCarts = cartRepository.findIdleCarts(
                CartStatus.ACTIVE,
                cutoff
        );

        for (Cart cart : idleCarts) {
            if (cartItemRepository.countByCartCartId(cart.getCartId()) == 0) {
                log.debug(
                        "Skipping empty idle cartId={}",
                        cart.getCartId()
                );
                continue;
            }

            LocalDateTime abandonedAt = LocalDateTime.now();
            cart.setCartStatus(CartStatus.ABANDONED);
            cart.setAbandonedAt(abandonedAt);
            cart.setUpdatedAt(abandonedAt);
            cart.setReminderSent(false);
            cartRepository.save(cart);
           AbandonedCartEvent event= new AbandonedCartEvent(
                   cart.getCartId(),
                   cart.getCustomer().getCustomerId(),
                   cart.getCustomer().getFirstName(),
                   cart.getCustomer().getPhone(),
                   cart.getCustomer().getEmail(),
                   cart.getTotalAmount(),
                   BigDecimal.ZERO,
                   "CART_ABANDONED"
           );
           kafkaCartEventProducer.sendAbandonedCartEvent(event);

            log.info(
                    "Detected abandoned cartId={} after {} of inactivity",
                    cart.getCartId(),
                    idleTimeout
            );
        }

        List<Cart> abandonedCarts = cartRepository
                .findByCartStatusAndReminderSentFalse(CartStatus.ABANDONED);

        for (Cart cart : abandonedCarts) {
            try {
                abandonedCartService.createForCart(cart.getCartId());
            } catch (Exception e) {
                log.error(
                        "Failed to create abandoned-cart record for cartId={}: {}",
                        cart.getCartId(),
                        e.getMessage(),
                        e
                );
            }
        }

        List<AbandonedCart> dueReminders =
                abandonedCartService.getDueFirstReminders(
                        firstReminderDelay
                );
        
        log.info("Found {} carts due for first reminder", dueReminders.size());

        for (AbandonedCart abandonedCart : dueReminders) {
            Long cartId = abandonedCart.getCart().getCartId();

            try {
                Customer customer = abandonedCart.getCustomer();
                Cart cart = abandonedCart.getCart();

AbandonedCartEvent event= new AbandonedCartEvent(
                                  cart.getCartId(),
        customer.getCustomerId(),
        customer.getFirstName(),
        customer.getPhone(),
        customer.getEmail(),
        cart.getTotalAmount(),
        abandonedCart.getDiscountAmount(),
        "FIRST_REMINDER"

);
  kafkaCartEventProducer.sendAbandonedCartEvent(event);

                log.info(
                        "Published FIRST_REMINDER Kafka event for cartId={}",
                        cartId
                );



            } catch (Exception exception) {
                log.error(
                        "Failed to send first reminder for cartId={}: {}",
                        cartId,
                        exception.getMessage(),
                        exception
                );
            }
        }
    }

    /**
     * Sends one second reminder after the configured delay from the first send.
     */
    @Scheduled(
            fixedDelayString = "#{T(java.time.Duration).parse('${abandoned.cart.scan-interval:PT30S}').toMillis()}",
            initialDelayString = "120000"
    )
    public void sendDueSecondReminders() {
        log.debug("Starting sendDueSecondReminders with delay: {}", secondReminderDelay);
        
        abandonedCartService.logAllAbandonedCarts();
        
        List<AbandonedCart> candidates =
                abandonedCartService.getDueSecondReminders(
                        secondReminderDelay
                );
        
        log.info("Found {} candidates for second reminder", candidates.size());

        for (AbandonedCart abandonedCart : candidates) {
            Cart cart = abandonedCart.getCart();

            Long cartId = cart.getCartId();
            log.debug("Processing second reminder for cartId={}, reminderSentAt={}, recoveryStatus={}", 
                    cartId, abandonedCart.getReminderSentAt(), abandonedCart.getRecoveryStatus());
            
            try {
                Customer customer = abandonedCart.getCustomer();

                AbandonedCartEvent event =
                        new AbandonedCartEvent(
                                cart.getCartId(),
                                customer.getCustomerId(),
                                customer.getFirstName(),
                                customer.getPhone(),
                                customer.getEmail(),
                                cart.getTotalAmount(),
                                abandonedCart.getDiscountAmount() == null
                                        ? BigDecimal.ZERO
                                        : abandonedCart.getDiscountAmount(),
                                "SECOND_REMINDER"
                        );

                kafkaCartEventProducer.sendAbandonedCartEvent(event);

                log.info(
                        "Published SECOND_REMINDER Kafka event for cartId={}",
                        cartId
                );


            } catch (Exception exception) {
                log.error(
                        "Failed to send second reminder for cartId={}: {}",
                        cartId,
                        exception.getMessage(),
                        exception
                );
            }
        }
    }

    /**
     * Sends one confirmation after a customer checks out a recovered cart.
     */
    @Scheduled(
            fixedDelayString = "#{T(java.time.Duration).parse('${abandoned.cart.scan-interval:PT30S}').toMillis()}",
            initialDelayString = "150000"
    )
    public void sendRecoveryConfirmations() {
        List<AbandonedCart> candidates =
                abandonedCartService.getPendingRecoveryConfirmations();

        for (AbandonedCart abandonedCart : candidates) {
            Long cartId = abandonedCart.getCart().getCartId();

            try {
                emailService.sendRecoveryConfirmation(cartId);
                log.info(
                        "Sent recovery confirmation for cartId={}",
                        cartId
                );
            } catch (Exception exception) {
                log.error(
                        "Failed to send recovery confirmation for cartId={}: {}",
                        cartId,
                        exception.getMessage(),
                        exception
                );
            }
        }
    }



}
