package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartItem;
import com.example.cartrecovery.entity.CartStatus;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.entity.Product;
import com.example.cartrecovery.repository.CartItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecoveryMessageService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryMessageService.class);

    private final CartItemRepository cartItemRepository;
    private final OpenAiRecoveryMessageClient aiClient;
    private final long highSalesThreshold;
    private final String offerDescription;

    private static final List<CartStatus> COMPLETED_PURCHASE_STATUSES = List.of(
            CartStatus.CHECKED_OUT,
            CartStatus.COMPLETED,
            CartStatus.RECOVERED
    );

    public RecoveryMessageService(
            CartItemRepository cartItemRepository,
            OpenAiRecoveryMessageClient aiClient,
            @Value("${ai.recovery.social-proof.high-sales-threshold:5}")
            long highSalesThreshold,
            @Value("${abandoned.cart.offer-description:Recovery discount available}")
            String offerDescription
    ) {
        this.cartItemRepository = cartItemRepository;
        this.aiClient = aiClient;
        this.highSalesThreshold = Math.max(1, highSalesThreshold);
        this.offerDescription = defaultText(
                offerDescription,
                "Recovery discount available"
        );
    }

    /**
     * Builds a minimal cart snapshot, asks AI for copy when enabled, and always
     * falls back to deterministic copy if the provider is disabled or fails.
     */
    public RecoveryMessage createMessage(
            Customer customer,
            Cart cart,
            AbandonedCart abandonedCart,
            ReminderStage stage
    ) {
        LocalDateTime purchasedSince = LocalDateTime.now().minusMonths(1);
        List<RecoveryMessage.ProductSummary> products = cartItemRepository
                .findByCartCartId(cart.getCartId())
                .stream()
                .filter(item -> item.getProduct() != null)
                .map(item -> toProductSummary(item, purchasedSince))
                .toList();

        boolean offerAvailable = abandonedCart.getDiscountCode() != null
                && !abandonedCart.getDiscountCode().isBlank();
        OpenAiRecoveryMessageClient.AiCopy copy = fallbackCopy(
                products,
                stage,
                offerAvailable
        );
        if (aiClient.isEnabled()) {
            try {
                OpenAiRecoveryMessageClient.AiCopy generated = aiClient.generate(
                        buildCartContext(customer, cart, abandonedCart, products, stage)
                );
                if (isValid(generated)) {
                    copy = normalize(generated);
                } else {
                    log.warn("AI returned unusable recovery copy for cartId={}; using fallback", cart.getCartId());
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "AI recovery copy failed for cartId={}; using fallback: {}",
                        cart.getCartId(),
                        exception.getMessage()
                );
            }
        }

        return new RecoveryMessage(
                copy.subject(),
                copy.headline(),
                copy.message(),
                products
        );
    }

    private Map<String, Object> buildCartContext(
            Customer customer,
            Cart cart,
            AbandonedCart abandonedCart,
            List<RecoveryMessage.ProductSummary> products,
            ReminderStage stage
    ) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("customer_first_name", safeCustomerName(customer));
        context.put("cart_total", cart.getTotalAmount());
        context.put("reminder_stage", stage.name());
        boolean offerAvailable = abandonedCart.getDiscountCode() != null
                && !abandonedCart.getDiscountCode().isBlank();
        context.put("offer_available", offerAvailable);
        context.put("offer_description", offerAvailable ? offerDescription : "None");
        context.put(
                "discount_percentage",
                offerAvailable && abandonedCart.getDiscountPercentage() != null
                        ? abandonedCart.getDiscountPercentage()
                        : 0
        );
        context.put("social_proof_rule",
                "Use only the supplied lastMonthCustomerCount, lastMonthUnitsPurchased, "
                        + "and highSales values. Never estimate or increase them.");
        context.put("products", products);
        return context;
    }

    private RecoveryMessage.ProductSummary toProductSummary(
            CartItem item,
            LocalDateTime purchasedSince
    ) {
        Product product = item.getProduct();
        BigDecimal price = item.getUnitPrice() != null
                ? item.getUnitPrice()
                : product.getPrice();
        int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
        String category = product.getCategory() == null
                ? "Uncategorized"
                : product.getCategory().name();
        long lastMonthCustomerCount = cartItemRepository
                .countCustomersWhoPurchasedProductSince(
                        product.getProductId(),
                        COMPLETED_PURCHASE_STATUSES,
                        purchasedSince
                );
        long lastMonthUnitsPurchased = cartItemRepository
                .countPurchasedUnitsForProductSince(
                        product.getProductId(),
                        COMPLETED_PURCHASE_STATUSES,
                        purchasedSince
                );
        return new RecoveryMessage.ProductSummary(
                defaultText(product.getProductName(), "Product"),
                category,
                price,
                quantity,
                lastMonthCustomerCount,
                lastMonthUnitsPurchased,
                lastMonthUnitsPurchased >= highSalesThreshold
        );
    }

    private OpenAiRecoveryMessageClient.AiCopy fallbackCopy(
            List<RecoveryMessage.ProductSummary> products,
            ReminderStage stage,
            boolean offerAvailable
    ) {
        String featuredProduct = products.isEmpty() ? "your items" : products.getFirst().name();
        String socialProof = buildSocialProof(products);
        String offer = offerAvailable
                ? " " + offerDescription + "—use the code shown below."
                : "";
        if (stage == ReminderStage.SECOND) {
            return new OpenAiRecoveryMessageClient.AiCopy(
                    "A second reminder: your cart is waiting",
                    "Still thinking it over?",
                    featuredProduct + " and the rest of your cart are still waiting for you."
                            + socialProof
                            + offer
            );
        }
        return new OpenAiRecoveryMessageClient.AiCopy(
                "You left something in your cart",
                "Your cart is waiting",
                "You left " + featuredProduct + " in your cart. Pick up where you left off."
                        + socialProof
                        + offer
        );
    }

    private String buildSocialProof(
            List<RecoveryMessage.ProductSummary> products
    ) {
        return products.stream()
                .filter(product -> product.lastMonthCustomerCount() > 0)
                .findFirst()
                .map(product -> {
                    String popularity = product.highSales()
                            ? " It is one of our popular products."
                            : "";
                    return " " + product.lastMonthCustomerCount()
                            + " customer(s) purchased "
                            + product.lastMonthUnitsPurchased()
                            + " unit(s) of this product in the last month."
                            + popularity;
                })
                .orElse("");
    }

    private boolean isValid(OpenAiRecoveryMessageClient.AiCopy copy) {
        return copy != null
                && hasLength(copy.subject(), 80)
                && hasLength(copy.headline(), 90)
                && hasLength(copy.message(), 320);
    }

    private boolean hasLength(String value, int maximum) {
        return value != null && !value.isBlank() && value.length() <= maximum;
    }

    private OpenAiRecoveryMessageClient.AiCopy normalize(
            OpenAiRecoveryMessageClient.AiCopy copy
    ) {
        return new OpenAiRecoveryMessageClient.AiCopy(
                copy.subject().replace('\r', ' ').replace('\n', ' ').trim(),
                copy.headline().trim(),
                copy.message().trim()
        );
    }

    private String safeCustomerName(Customer customer) {
        return customer == null ? "Customer" : defaultText(customer.getFirstName(), "Customer");
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public enum ReminderStage {
        FIRST,
        SECOND
    }
}
