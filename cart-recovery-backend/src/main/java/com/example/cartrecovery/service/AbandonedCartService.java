package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.*;
import com.example.cartrecovery.exception.AbandonedCartNotFoundException;
import com.example.cartrecovery.exception.CartNotFoundException;
import com.example.cartrecovery.repository.AbandonedCartRepository;
import com.example.cartrecovery.repository.CartRepository;
import com.example.cartrecovery.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.example.cartrecovery.util.DiscountCodeGenerator;

@Service
@Transactional
public class AbandonedCartService {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartService.class);
    private static final String SECOND_REMINDER_TYPE = "EMAIL_REMINDER_2";
    private static final String RECOVERY_CONFIRMATION_TYPE =
            "EMAIL_RECOVERY_CONFIRMATION";

    private final AbandonedCartRepository abandonedCartRepository;
    private final CartRepository cartRepository;
    private final NotificationRepository notificationRepository;

    public AbandonedCartService(
            AbandonedCartRepository abandonedCartRepository,
            CartRepository cartRepository,
            NotificationRepository notificationRepository
    ) {
        this.abandonedCartRepository = abandonedCartRepository;
        this.cartRepository = cartRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create an AbandonedCart record for the provided cart if one does not exist.
     */
    public AbandonedCart createForCart(Long cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        Optional<AbandonedCart> existing = abandonedCartRepository.findByCartCartId(cartId);
        if (existing.isPresent()) {
            AbandonedCart abandonedCart = existing.get();
            abandonedCart.setCart(cart);
            abandonedCart.setCustomer(cart.getCustomer());
            abandonedCart.setNotificationChannel(NotificationChannel.EMAIL);
            abandonedCart.setAbandonedAt(
                    cart.getAbandonedAt() == null
                            ? LocalDateTime.now()
                            : cart.getAbandonedAt()
            );
            abandonedCart.setReminderSent(false);
            abandonedCart.setReminderSentAt(null);
            abandonedCart.setReminderCount(0);
            abandonedCart.setRecoveryStatus("PENDING");


            return abandonedCartRepository.save(abandonedCart);
        }

        AbandonedCart ac = new AbandonedCart();
        ac.setCart(cart);
        Customer customer = cart.getCustomer();
        ac.setCustomer(customer);
        ac.setNotificationChannel(NotificationChannel.EMAIL);
        ac.setAbandonedAt(
                cart.getAbandonedAt() == null
                        ? LocalDateTime.now()
                        : cart.getAbandonedAt()
        );
        ac.setReminderSent(false);
        ac.setReminderCount(0);
        ac.setRecovered(false);

        ac.setRecoveryStatus("PENDING");

        return abandonedCartRepository.save(ac);
    }

    /**
     * Resets an existing recovery cycle when the customer returns and changes
     * the cart. A normal active cart may not have an abandoned-cart row yet.
     */
    public void resetForCustomerActivity(Long cartId) {
        abandonedCartRepository.findByCartCartId(cartId)
                .ifPresent(abandonedCart -> {
                    abandonedCart.setRecoveryStatus("PENDING");
                    abandonedCart.setReminderSent(false);
                    abandonedCart.setReminderSentAt(null);
                    abandonedCart.setReminderCount(0);
                    abandonedCart.setDiscountCode(null);
                    abandonedCart.setDiscountPercentage(null);
                    abandonedCart.setRecovered(false);
                    abandonedCart.setRecoveredAt(null);
                    abandonedCart.setRecoveredTime(null);
                    abandonedCartRepository.save(abandonedCart);
                });
    }

    public AbandonedCart getByCartId(Long cartId) {
        return abandonedCartRepository.findByCartCartId(cartId)
                .orElseThrow(() -> new AbandonedCartNotFoundException(cartId));
    }

    public List<AbandonedCart> getByCustomerId(Long customerId) {
        return abandonedCartRepository.findByCustomerCustomerId(customerId);
    }

    public List<AbandonedCart> getPendingReminders() {
        return abandonedCartRepository.findByReminderSentFalse();
    }

    public List<AbandonedCart> getSentReminders() {
        return abandonedCartRepository.findByReminderSentTrue();
    }

    /**
     * Finds recovered carts that do not yet have a successfully sent recovery
     * confirmation.
     */
    public List<AbandonedCart> getPendingRecoveryConfirmations() {
        return abandonedCartRepository.findByRecoveryStatus("RECOVERED")
                .stream()
                .filter(abandonedCart -> abandonedCart.getCart() != null)
                .filter(abandonedCart -> abandonedCart.getCart()
                        .getCartStatus() == CartStatus.RECOVERED)
                .filter(abandonedCart -> !notificationRepository
                        .existsByCartCartIdAndNotificationTypeAndNotificationStatus(
                                abandonedCart.getCart().getCartId(),
                                RECOVERY_CONFIRMATION_TYPE,
                                "SENT"
                        ))
                .toList();
    }

    /**
     * Finds abandoned carts that have waited long enough for their first
     * reminder and have not already received it.
     */
    public List<AbandonedCart> getDueFirstReminders(Duration delay) {
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException(
                    "First reminder delay must not be negative."
            );
        }

        LocalDateTime cutoff = LocalDateTime.now().minus(delay);

        return abandonedCartRepository
                .findByReminderSentFalseAndAbandonedAtLessThanEqualAndRecoveryStatusNot(
                        cutoff,
                        "RECOVERED"
                )
                .stream()
                .filter(abandonedCart -> abandonedCart.getCart() != null)
                .filter(abandonedCart -> abandonedCart.getCart().getCartStatus()
                        == CartStatus.ABANDONED)
                .toList();
    }

    /**
     * Finds carts whose first reminder was sent long enough ago and whose
     * second reminder has not succeeded yet.
     */
    public List<AbandonedCart> getDueSecondReminders(Duration delay) {
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException(
                    "Second reminder delay must not be negative."
            );
        }

        LocalDateTime cutoff = LocalDateTime.now().minus(delay);
        log.info("getDueSecondReminders: delay={}, cutoff={}, looking for carts with reminderSentAt <= cutoff",
                delay, cutoff);

        // First, let's check all carts with reminderSent=true to understand the data
        List<AbandonedCart> allReminderSent = abandonedCartRepository.findByReminderSentTrue();
        log.info("getDueSecondReminders: DEBUG - Found {} total carts with reminderSent=true", allReminderSent.size());
        allReminderSent.forEach(ac -> {
            Cart cart = ac.getCart();
            if (cart != null) {
                long diffMs = ac.getReminderSentAt() != null ?
                        Duration.between(ac.getReminderSentAt(), LocalDateTime.now()).toMillis() :-1;
                log.info("  - cartId={}, reminderSentAt={}, cart.status={}, recovery.status={}, time since sent={} ms",
                        cart.getCartId(), ac.getReminderSentAt(), cart.getCartStatus(), ac.getRecoveryStatus(), diffMs);
            }
        });

        log.info("getDueSecondReminders: Looking for cutoff={}, so need reminderSentAt <= {}", cutoff, cutoff);

        List<AbandonedCart> allCandidates = abandonedCartRepository
                .findByReminderSentTrueAndReminderSentAtLessThanEqualAndRecoveryStatusNot(
                        cutoff,
                        "RECOVERED"
                );

        log.info("getDueSecondReminders: Found {} carts matching query (reminderSent=true AND reminderSentAt<={} AND recoveryStatus!=RECOVERED)",
                allCandidates.size(), cutoff);

        if (!allCandidates.isEmpty()) {
            allCandidates.forEach(ac -> {
                Cart cart = ac.getCart();
                if (cart != null) {
                    log.info("  ✓ Query result - cartId={}, reminderSentAt={}, cart.status={}, recovery.status={}",
                            cart.getCartId(), ac.getReminderSentAt(), cart.getCartStatus(), ac.getRecoveryStatus());
                }
            });
        } else {
            log.info("getDueSecondReminders: NO RESULTS from database query. This means:");
            log.info("  - Either no carts have reminderSent=true");
            log.info("  - Or all have reminderSentAt > cutoff (sent too recently)");
            log.info("  - Or all have recoveryStatus=RECOVERED");
        }

        List<AbandonedCart> filteredByAwaitingRecovery = allCandidates.stream()
                .filter(this::isAwaitingRecovery)
                .peek(ac -> log.info("    ✓ cartId={} passed isAwaitingRecovery check (cart status is ABANDONED or REMINDER_SENT)",
                        ac.getCart().getCartId()))
                .toList();

        log.info("getDueSecondReminders: After isAwaitingRecovery filter: {} candidates",
                filteredByAwaitingRecovery.size());

        List<AbandonedCart> result = filteredByAwaitingRecovery.stream()
                .filter(abandonedCart -> {
                    boolean hasNoSentNotification =
                            !hasSecondReminderBeenSentInCurrentCycle(
                                    abandonedCart
                            );
                    if (!hasNoSentNotification) {
                        log.info("    ✗ cartId={} already has SENT EMAIL_REMINDER_2 notification",
                                abandonedCart.getCart().getCartId());
                    } else {
                        log.info("    ✓ cartId={} ready for second reminder (no SENT EMAIL_REMINDER_2 yet)",
                                abandonedCart.getCart().getCartId());
                    }
                    return hasNoSentNotification;
                })
                .toList();

        log.info("getDueSecondReminders: Final result: {} carts ready for second reminder", result.size());

        return result;
    }

    /**
     * Checks only the current abandonment cycle. A cart can be abandoned more
     * than once, so a second reminder from an older cycle must not suppress the
     * follow-up for the current first reminder.
     */
    public boolean hasSecondReminderBeenSentInCurrentCycle(Long cartId) {
        return hasSecondReminderBeenSentInCurrentCycle(getByCartId(cartId));
    }

    private boolean hasSecondReminderBeenSentInCurrentCycle(
            AbandonedCart abandonedCart) {
        if (abandonedCart.getCart() == null
                || abandonedCart.getReminderSentAt() == null) {
            return false;
        }

        return notificationRepository
                .existsByCartCartIdAndNotificationTypeAndNotificationStatusAndSentAtGreaterThanEqual(
                        abandonedCart.getCart().getCartId(),
                        SECOND_REMINDER_TYPE,
                        "SENT",
                        abandonedCart.getReminderSentAt()
                );
    }

    /**
     * Finds unrecovered carts whose second reminder succeeded long enough ago.
     */
    public List<AbandonedCart> getDueExpirations(Duration expirationDelay) {
        if (expirationDelay == null || expirationDelay.isNegative()) {
            throw new IllegalArgumentException(
                    "Cart expiration delay must not be negative."
            );
        }

        LocalDateTime cutoff = LocalDateTime.now().minus(expirationDelay);

        return abandonedCartRepository.findDueAfterSuccessfulNotification(
                cutoff,
                SECOND_REMINDER_TYPE,
                "SENT",
                CartStatus.REMINDER_SENT,
                "RECOVERED"
        );
    }

    private boolean isAwaitingRecovery(AbandonedCart abandonedCart) {
        if (abandonedCart.getCart() == null) {
            return false;
        }

        CartStatus status = abandonedCart.getCart().getCartStatus();
        return status == CartStatus.ABANDONED
                || status == CartStatus.REMINDER_SENT;
    }

    public AbandonedCart ensureDiscountCode(
            Long cartId,
            String codePrefix,
            int discountPercentage
    ) {
        AbandonedCart abandonedCart = getByCartId(cartId);

        if (discountPercentage <= 0 || discountPercentage > 100) {
            throw new IllegalArgumentException(
                    "Discount percentage must be between 1 and 100."
            );
        }

        if (abandonedCart.getDiscountCode() != null
                && !abandonedCart.getDiscountCode().isBlank()) {
            if (!Integer.valueOf(discountPercentage).equals(
                    abandonedCart.getDiscountPercentage()
            )) {
                abandonedCart.setDiscountPercentage(discountPercentage);
                return abandonedCartRepository.save(abandonedCart);
            }
            return abandonedCart;
        }

        String normalizedPrefix = codePrefix == null
                ? "RECOVER"
                : codePrefix
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");

        if (normalizedPrefix.isBlank()) {
            normalizedPrefix = "RECOVER";
        }

        abandonedCart.setDiscountCode(DiscountCodeGenerator.generate(normalizedPrefix));
        abandonedCart.setDiscountPercentage(discountPercentage);

        return abandonedCartRepository.save(abandonedCart);
    }

    public AbandonedCart storeDiscountCode(Long cartId, String discountCode, int discountPercentage) {
        if (discountCode == null || discountCode.isBlank()) {
            throw new IllegalArgumentException("Discount code must not be empty.");
        }
        if (discountPercentage <= 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 1 and 100.");
        }
        AbandonedCart abandonedCart = getByCartId(cartId);
        abandonedCart.setDiscountCode(discountCode);
        abandonedCart.setDiscountPercentage(discountPercentage);
        return abandonedCartRepository.save(abandonedCart);
    }

    public AbandonedCart markReminderSent(Long cartId) {
        AbandonedCart ac = getByCartId(cartId);
        LocalDateTime sentAt = LocalDateTime.now();

        ac.setReminderSent(true);
        ac.setReminderSentAt(sentAt);
        ac.setReminderCount(1);
        ac.setRecoveryStatus("REMINDER_SENT");

        Cart cart = ac.getCart();
        if (cart != null) {
            cart.setReminderSent(true);
            cart.setCartStatus(CartStatus.REMINDER_SENT);
            cart.setUpdatedAt(sentAt);
            cartRepository.save(cart);
        }

        AbandonedCart saved = abandonedCartRepository.save(ac);
        log.info("markReminderSent: cartId={}, reminderSent={}, reminderSentAt={}, recoveryStatus={}",
                cartId, saved.getReminderSent(), saved.getReminderSentAt(), saved.getRecoveryStatus());
        return saved;
    }
public AbandonedCart markSecondReminderSent(Long cartId){
        AbandonedCart ac= getByCartId(cartId);
        ac.setReminderCount(2);
        AbandonedCart saved=abandonedCartRepository.save(ac);
return  saved;
}

    public AbandonedCart incrementReminderCount(Long cartId) {
        AbandonedCart ac = getByCartId(cartId);
        int currentCount = ac.getReminderCount() != null ? ac.getReminderCount() : 0;
        ac.setReminderCount(currentCount + 1);
        return abandonedCartRepository.save(ac);
    }


    public AbandonedCart markRecovered(Long cartId) {
        AbandonedCart ac = getByCartId(cartId);
        LocalDateTime recoveredAt = LocalDateTime.now();

        ac.setRecoveryStatus("RECOVERED");
        ac.setRecovered(true);
        ac.setRecoveredAt(recoveredAt);
        ac.setRecoveredTime(recoveredAt);

        return abandonedCartRepository.save(ac);
    }

    public AbandonedCart markExpired(Long cartId) {
        AbandonedCart abandonedCart = getByCartId(cartId);
        Cart cart = abandonedCart.getCart();

        if (cart == null) {
            throw new IllegalStateException(
                    "Cart is not available for abandoned cart ID: "
                            + abandonedCart.getAbandonedCartId()
            );
        }

        if ("RECOVERED".equalsIgnoreCase(
                abandonedCart.getRecoveryStatus()
        ) || cart.getCartStatus() == CartStatus.RECOVERED
                || cart.getCartStatus() == CartStatus.CHECKED_OUT
                || cart.getCartStatus() == CartStatus.COMPLETED) {
            throw new IllegalStateException(
                    "A checked-out or recovered cart cannot be expired."
            );
        }

        LocalDateTime expiredAt = LocalDateTime.now();
        cart.setCartStatus(CartStatus.EXPIRED);
        cart.setUpdatedAt(expiredAt);
        cartRepository.save(cart);

        abandonedCart.setRecoveryStatus("EXPIRED");
        return abandonedCartRepository.save(abandonedCart);
    }

    public void logAllAbandonedCarts() {
        List<AbandonedCart> allCarts = abandonedCartRepository.findAll();
        log.info("==== ALL ABANDONED CARTS IN DATABASE ({} total) ====", allCarts.size());
        if (allCarts.isEmpty()) {
            log.info("No abandoned carts found");
            return;
        }

        allCarts.forEach(ac -> {
            Cart cart = ac.getCart();
            log.info("cartId={}, reminderSent={}, reminderSentAt={}, reminderCount={}, recoveryStatus={}, cart.status={}, cart.abandonedAt={}, cart.updatedAt={}",
                    cart.getCartId(),
                    ac.getReminderSent(),
                    ac.getReminderSentAt(),
                    ac.getReminderCount(),
                    ac.getRecoveryStatus(),
                    cart.getCartStatus(),
                    cart.getAbandonedAt(),
                    cart.getUpdatedAt()
            );
        });
        log.info("==== END ABANDONED CARTS ====");
    }

    public boolean existsForCart(Long cartId) {
        return abandonedCartRepository.existsByCartCartId(cartId);
    }
}
