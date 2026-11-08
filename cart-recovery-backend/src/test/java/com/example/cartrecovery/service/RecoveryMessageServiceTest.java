package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartItem;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.entity.Product;
import com.example.cartrecovery.entity.ProductCategory;
import com.example.cartrecovery.repository.CartItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryMessageServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OpenAiRecoveryMessageClient aiClient;

    private RecoveryMessageService service;
    private Customer customer;
    private Cart cart;
    private AbandonedCart abandonedCart;

    @BeforeEach
    void setUp() {
        service = new RecoveryMessageService(
                cartItemRepository,
                aiClient,
                5,
                "Save 10% with your recovery offer"
        );

        customer = new Customer();
        customer.setFirstName("Alex");
        customer.setEmail("alex@example.com");

        cart = new Cart();
        cart.setCartId(7L);
        cart.setCustomer(customer);
        cart.setTotalAmount(new BigDecimal("1299.00"));

        abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setCustomer(customer);
    }

    @Test
    void sendsOfferAndVerifiedSalesContextToAi() {
        Product product = new Product();
        product.setProductId(99L);
        product.setProductName("Developer Laptop");
        product.setCategory(ProductCategory.LAPTOP);
        product.setPrice(new BigDecimal("1299.00"));

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setUnitPrice(new BigDecimal("1199.00"));
        item.setQuantity(1);

        abandonedCart.setDiscountCode("RECOVER-7-TEST");
        abandonedCart.setDiscountPercentage(10);

        when(cartItemRepository.findByCartCartId(7L)).thenReturn(List.of(item));
        when(cartItemRepository.countCustomersWhoPurchasedProductSince(
                eq(99L),
                anyList(),
                any(LocalDateTime.class)
        )).thenReturn(12L);
        when(cartItemRepository.countPurchasedUnitsForProductSince(
                eq(99L),
                anyList(),
                any(LocalDateTime.class)
        )).thenReturn(25L);
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generate(org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(new OpenAiRecoveryMessageClient.AiCopy(
                        "Your laptop is waiting",
                        "Finish setting up your next project",
                        "Your selected laptop is still saved in your cart."
                ));

        RecoveryMessage message = service.createMessage(
                customer,
                cart,
                abandonedCart,
                RecoveryMessageService.ReminderStage.FIRST
        );

        assertEquals("Your laptop is waiting", message.subject());
        assertEquals("LAPTOP", message.products().getFirst().category());
        assertEquals(new BigDecimal("1199.00"), message.products().getFirst().unitPrice());
        assertEquals(12L, message.products().getFirst().lastMonthCustomerCount());
        assertEquals(25L, message.products().getFirst().lastMonthUnitsPurchased());
        assertTrue(message.products().getFirst().highSales());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> contextCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(aiClient).generate(contextCaptor.capture());
        Map<String, Object> context = contextCaptor.getValue();
        assertEquals("Alex", context.get("customer_first_name"));
        assertEquals(new BigDecimal("1299.00"), context.get("cart_total"));
        assertEquals("FIRST", context.get("reminder_stage"));
        assertTrue((Boolean) context.get("offer_available"));
        assertEquals(
                "Save 10% with your recovery offer",
                context.get("offer_description")
        );
        assertEquals(10, context.get("discount_percentage"));
    }

    @Test
    void usesDeterministicFallbackWhenAiIsDisabled() {
        when(cartItemRepository.findByCartCartId(7L)).thenReturn(List.of());
        when(aiClient.isEnabled()).thenReturn(false);

        RecoveryMessage message = service.createMessage(
                customer,
                cart,
                abandonedCart,
                RecoveryMessageService.ReminderStage.FIRST
        );

        assertEquals("You left something in your cart", message.subject());
        assertEquals("Your cart is waiting", message.headline());
    }
}
