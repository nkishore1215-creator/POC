package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.*;
import com.example.cartrecovery.repository.CartItemRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class EmailService {

    private static final String FIRST_REMINDER_TYPE = "EMAIL_REMINDER_1";
    private static final String SECOND_REMINDER_TYPE = "EMAIL_REMINDER_2";
    private static final String RECOVERY_CONFIRMATION_TYPE =
            "EMAIL_RECOVERY_CONFIRMATION";

    private final JavaMailSender javaMailSender;
    private final NotificationService notificationService;
    private final AbandonedCartService abandonedCartService;
    private final CartItemRepository cartItemRepository;
    private final RecoveryMessageService recoveryMessageService;
    private final RecoveryDiscountPolicy recoveryDiscountPolicy;


    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.password:}")
    private String senderPassword;

    @Value("${app.frontend.cart-url:http://localhost:3000/cart}")
    private String cartPageUrl;
        
    @Value("${abandoned.cart.discount-code-prefix:RECOVER}")
    private String discountCodePrefix;

    @Value("${abandoned.cart.first-reminder-offer-enabled:false}")
    private boolean firstReminderOfferEnabled;

    @Value("${abandoned.cart.accessory-offer-message:Complete your setup and explore special offers on other accessories.}")
    private String accessoryOfferMessage;

    public EmailService(
            JavaMailSender javaMailSender,
            NotificationService notificationService,
            AbandonedCartService abandonedCartService,
            CartItemRepository cartItemRepository,
            RecoveryMessageService recoveryMessageService,
            RecoveryDiscountPolicy recoveryDiscountPolicy
             ) {

        this.javaMailSender = javaMailSender;
        this.notificationService = notificationService;
        this.abandonedCartService = abandonedCartService;
        this.cartItemRepository = cartItemRepository;
        this.recoveryMessageService = recoveryMessageService;
        this.recoveryDiscountPolicy = recoveryDiscountPolicy;

    }

    /**
     * Sends a basic email.
     */
    public void sendEmail(
            String recipientEmail,
            String subject,
            String emailBody) {

        validateEmail(recipientEmail);
        validateMailConfiguration();
        try{
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom("Shopping Cart <"+senderEmail+">");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(emailBody, true);
             javaMailSender.send(message);
        }catch (MessagingException e){
            throw new IllegalStateException("Unable to send email",e);

        }

    }

    /**
     * Sends an abandoned-cart reminder.
     */
    public Notification sendAbandonedCartReminder(
            Long abandonedCartId) {

        AbandonedCart abandonedCart =
                abandonedCartService.getByCartId(
                        abandonedCartId
                );

        Cart cart = abandonedCart.getCart();
        Customer customer = abandonedCart.getCustomer();

        if (customer == null) {
            throw new IllegalStateException(
                    "Customer is not available for abandoned cart ID: "
                            + abandonedCartId
            );
        }

        if (!customer.isEmailOptIn()) {
            throw new IllegalStateException(
                    "Customer has not opted in for email notifications."
            );
        }

        validateEmail(customer.getEmail());
        validateMailConfiguration();

        if (Boolean.TRUE.equals(abandonedCart.getReminderSent())) {
            throw new IllegalArgumentException("Reminder has already been sent for abandoned cart ID:"+cart.getCartId()
                    );

        }

            abandonedCart = addEligibleDiscount(abandonedCartId, cart);


        Notification notification =
                notificationService.createNotificationForCart(
                        cart.getCartId(),
                        FIRST_REMINDER_TYPE
                );

        Long notificationId = notification.getNotificationId();
        if (notificationId == null) {
            throw new IllegalStateException(
                    "Failed to create notification record for cart ID: "
                            + cart.getCartId()
            );
        }

        RecoveryMessage recoveryMessage = recoveryMessageService.createMessage(
                customer,
                cart,
                abandonedCart,
                RecoveryMessageService.ReminderStage.FIRST
        );
        String emailBody = buildRecoveryEmail(
                customer,
                cart,
                abandonedCart,
                recoveryMessage
        );

        try {
   
            sendEmail(
                    customer.getEmail(),
                    recoveryMessage.subject(),
                    emailBody
            );

            Notification sentNotification =
                    notificationService.markSent(notificationId);

            abandonedCartService.markReminderSent(
                    abandonedCartId
            );

            return sentNotification;

        } catch (RuntimeException exception) {

            notificationService.markFailed(notificationId);

            throw new IllegalStateException(
                    "Unable to send abandoned-cart reminder email.",
                    exception
            );
        }
    }



    /**
     * Sends one follow-up email after the first reminder was sent.
     */
    public Notification sendSecondAbandonedCartReminder(Long cartId) {

        AbandonedCart abandonedCart = abandonedCartService.getByCartId(cartId);
        Cart cart = abandonedCart.getCart();
        Customer customer = abandonedCart.getCustomer();

        validateReminderCustomer(customer, cartId);
        validateMailConfiguration();
        validateCartAwaitingRecovery(abandonedCart, cart, cartId);

        if (!Boolean.TRUE.equals(abandonedCart.getReminderSent())) {
            throw new IllegalStateException(
                    "Send the first reminder before sending the second reminder."
            );
        }

        if (abandonedCartService
                .hasSecondReminderBeenSentInCurrentCycle(cartId)) {
            throw new IllegalStateException(
                    "The second reminder has been sent successfully for : "+cartId

            );
        }

        abandonedCart = addEligibleDiscount(cartId, cart);

        Notification notification =
                notificationService.createNotificationForCart(
                        cartId,
                        SECOND_REMINDER_TYPE
                );

        Long notificationId = notification.getNotificationId();
        if (notificationId == null) {
            throw new IllegalStateException(
                    "Failed to create the second reminder for cart ID: "
                            + cartId
            );
        }

        RecoveryMessage recoveryMessage = recoveryMessageService.createMessage(
                customer,
                cart,
                abandonedCart,
                RecoveryMessageService.ReminderStage.SECOND
        );
        String emailBody = buildRecoveryEmail(
                customer,
                cart,
                abandonedCart,
                recoveryMessage
        );

        try {
            sendEmail(
                    customer.getEmail(),
                    recoveryMessage.subject(),
                    emailBody
            );

            return notificationService.markSent(notificationId);

        } catch (RuntimeException exception) {
            notificationService.markFailed(notificationId);

            throw new IllegalStateException(
                    "Unable to send the second abandoned-cart reminder email.",
                    exception
            );
        }
    }

    public boolean hasSecondReminderBeenSent(Long cartId) {
        return abandonedCartService
                .hasSecondReminderBeenSentInCurrentCycle(cartId);
    }

    /**
     * get Product name based on cart.
     */
    private String getProduct(Long cartId) {
        List<CartItem> cartItem = cartItemRepository.findByCartCartId(cartId);
       if(cartItem==null || cartItem.isEmpty()){
           return "No Product Found";
       }
return   cartItem.stream().filter(
        item -> item.getProduct()!=null)
        .map(item ->
                item.getProduct().getProductName()
        )
        .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Sends one confirmation after an abandoned cart is recovered by checkout.
     */
    public Notification sendRecoveryConfirmation(Long cartId) {
        AbandonedCart abandonedCart =
                abandonedCartService.getByCartId(cartId);
        Cart cart = abandonedCart.getCart();
        Customer customer = abandonedCart.getCustomer();

        validateReminderCustomer(customer, cartId);
        validateMailConfiguration();

        if (cart == null
                || cart.getCartStatus() != CartStatus.RECOVERED
                || !"RECOVERED".equalsIgnoreCase(
                        abandonedCart.getRecoveryStatus()
                )) {
            throw new IllegalStateException(
                    "Cart ID " + cartId + " has not been recovered."
            );
        }

        if (notificationService.hasSentNotification(
                cartId,
                RECOVERY_CONFIRMATION_TYPE
        )) {
            throw new IllegalStateException(
                    "Recovery confirmation has already been sent for cart ID: "
                            + cartId
            );
        }

        Notification notification =
                notificationService.createNotificationForCart(
                        cartId,
                        RECOVERY_CONFIRMATION_TYPE
                );

        Long notificationId = notification.getNotificationId();
        if (notificationId == null) {
            throw new IllegalStateException(
                    "Failed to create recovery confirmation for cart ID: "
                            + cartId
            );
        }

        try {
            sendEmail(
                    customer.getEmail(),
                    "Thank you for completing your purchase!",
                    buildRecoveryConfirmationEmail(customer, cart)
            );

            return notificationService.markSent(notificationId);
        } catch (RuntimeException exception) {
            notificationService.markFailed(notificationId);
            throw new IllegalStateException(
                    "Unable to send the recovery confirmation email.",
                    exception
            );
        }
    }

    /**
     * Retries a failed notification.
     */
    public Notification retryFailedEmail(
            Long notificationId,
            Long abandonedCartId) {

        Notification notification =
                notificationService.retryNotification(
                        notificationId
                );

        AbandonedCart abandonedCart =
                abandonedCartService.getByCartId(
                        abandonedCartId
                );

        Customer customer = abandonedCart.getCustomer();
        Cart cart = abandonedCart.getCart();

        validateReminderCustomer(customer, abandonedCartId);
        validateMailConfiguration();

        RecoveryMessage recoveryMessage = recoveryMessageService.createMessage(
                customer,
                cart,
                abandonedCart,
                RecoveryMessageService.ReminderStage.FIRST
        );
        String emailBody = buildRecoveryEmail(
                customer,
                cart,
                abandonedCart,
                recoveryMessage
        );

        try {
            sendEmail(
                    customer.getEmail(),
                    recoveryMessage.subject(),
                    emailBody
            );

            Notification sentNotification =
                    notificationService.markSent(
                            notification.getNotificationId()
                    );

            abandonedCartService.markReminderSent(
                    abandonedCartId
            );

            return sentNotification;

        } catch (RuntimeException exception) {

            notificationService.markFailed(
                    notification.getNotificationId()
            );

            throw new IllegalStateException(
                    "Unable to resend abandoned-cart reminder email.",
                    exception
            );
        }
    }

    private String buildRecoveryEmail(
            Customer customer,
            Cart cart,
            AbandonedCart abandonedCart,
            RecoveryMessage recoveryMessage) {
        String customerName = customer.getFirstName() == null
                || customer.getFirstName().isBlank()
                ? "Customer"
                : customer.getFirstName();

        StringBuilder body = new StringBuilder();
        body.append("<p>Hi <b>")
                .append(escape(customerName))
                .append("</b>,</p>")
                .append("<h2>")
                .append(escape(recoveryMessage.headline()))
                .append("</h2>")
                .append("<p>")
                .append(escape(recoveryMessage.message()))
                .append("</p>");

        if (!recoveryMessage.products().isEmpty()) {
            body.append("<ul>");
            for (RecoveryMessage.ProductSummary product : recoveryMessage.products()) {
                body.append("<li><b>")
                        .append(escape(product.name()))
                        .append("</b> &mdash; ")
                        .append(escape(product.category()))
                        .append(", quantity ")
                        .append(product.quantity());
                if (product.unitPrice() != null) {
                    body.append(" at $").append(product.unitPrice());
                }
                if (product.lastMonthCustomerCount() > 0) {
                    body.append(". Purchased by ")
                            .append(product.lastMonthCustomerCount())
                            .append(" customer(s) in the last month, ")
                            .append(product.lastMonthUnitsPurchased())
                            .append(" unit(s) sold");
                }
                if (product.highSales()) {
                    body.append(" &mdash; <b>Popular item</b>");
                }
                body.append("</li>");
            }
            body.append("</ul>");
        }

        body.append("<p><b>Cart total:</b> $")
                .append(cart.getTotalAmount())
                .append("</p>");

        if (accessoryOfferMessage != null && !accessoryOfferMessage.isBlank()) {
            body.append("<p>")
                    .append(escape(accessoryOfferMessage))
                    .append("</p>");
        }

        if (abandonedCart.getDiscountCode() != null
                && !abandonedCart.getDiscountCode().isBlank()) {
            if (abandonedCart.getDiscountPercentage() != null
                    && abandonedCart.getDiscountPercentage() > 0) {
                body.append("<p><b>Your discount:</b> ")
                        .append(abandonedCart.getDiscountPercentage())
                        .append("% off</p>");
            }
            body.append("<p>Use discount code: <b>")
                    .append(escape(abandonedCart.getDiscountCode()))
                    .append("</b></p>");

        }

        String recoveryUrl = buildRecoveryUrl(cart.getCartId());
        body.append("<p><a href=\"")
                .append(escape(recoveryUrl))
                .append("\">Complete your purchase</a></p>")
                .append("<p>Thank you,<br>Cart Recovery Team</p>");

        return body.toString();
    }

    private AbandonedCart addEligibleDiscount(Long cartId, Cart cart) {
        int percentage = recoveryDiscountPolicy.percentageFor(
                cart.getTotalAmount()
        );
        if (percentage == 0) {
            return abandonedCartService.getByCartId(cartId);
        }
        return abandonedCartService.ensureDiscountCode(
                cartId,
                discountCodePrefix,
                percentage
        );
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private String buildRecoveryConfirmationEmail(
            Customer customer,
            Cart cart
    ) {
        String productName = getProduct(cart.getCartId());
        String customerName =
                customer.getFirstName() == null
                        || customer.getFirstName().isBlank()
                        ? "Customer"
                        : customer.getFirstName();


        StringBuilder body = new StringBuilder();
        body.append("<p>Hi <b>")
                .append(escape(customerName))
                .append("</b>,</p>")
                .append("<p>Thank you for completing your purchase.</p>")
                .append("<p>We're happy you came back and finished your order.</p>")
                .append("<p><b>Cart ID:</b> ")
                .append(cart.getCartId())
                .append("<br><b>Product(s) :</b> ")
                .append("<br><ul><li>")
                .append(escape(productName))
                .append("</li></ul>");
             /*   .append(escape(productName));*/

        if (cart.getTotalAmount() != null) {
            body.append("<br><b>Order total:</b> $")
                    .append(cart.getTotalAmount());
        }

        return body.append("</p>")
                .append("<p>Thank you for shopping with us!</p>")
                .append("<p>Thank you,<br>Cart Recovery Team</p>")
                .toString();
    }

    private String buildRecoveryUrl(Long cartId) {
        return cartPageUrl + "/cart/" + cartId;
    }

    private void validateReminderCustomer(Customer customer, Long cartId) {
        if (customer == null) {
            throw new IllegalStateException(
                    "Customer is not available for cart ID: " + cartId
            );
        }

        if (!customer.isEmailOptIn()) {
            throw new IllegalStateException(
                    "Customer has not opted in for email notifications."
            );
        }

        validateEmail(customer.getEmail());
    }

    private void validateCartAwaitingRecovery(
            AbandonedCart abandonedCart,
            Cart cart,
            Long cartId) {

        if (cart == null) {
            throw new IllegalStateException(
                    "Cart is not available for abandoned cart ID: " + cartId
            );
        }

        CartStatus cartStatus = cart.getCartStatus();
        boolean awaitingRecovery = cartStatus == CartStatus.ABANDONED
                || cartStatus == CartStatus.REMINDER_SENT;

        if ("RECOVERED".equalsIgnoreCase(abandonedCart.getRecoveryStatus())
                || !awaitingRecovery) {
            throw new IllegalStateException(
                    "Cannot send a reminder because cart ID "
                            + cartId
                            + " is no longer abandoned."
            );
        }
    }

    private void validateEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email must not be empty."
            );
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException(
                    "Invalid recipient email address: " + email
            );
        }
    }

    private void validateMailConfiguration() {
        if (senderEmail == null || senderEmail.isBlank()
                || senderPassword == null || senderPassword.isBlank()) {
            throw new IllegalStateException(
                    "Email is not configured. Set MAIL_USERNAME and "
                            + "MAIL_APP_PASSWORD in the application run environment."
            );
        }
    }
}
