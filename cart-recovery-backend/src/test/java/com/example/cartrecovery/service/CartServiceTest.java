package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartItem;
import com.example.cartrecovery.entity.CartStatus;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.entity.Product;
import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.repository.CartItemRepository;
import com.example.cartrecovery.repository.CartRepository;
import com.example.cartrecovery.repository.CustomerRepository;
import com.example.cartrecovery.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AbandonedCartService abandonedCartService;

    @Mock
    private RecoveryDiscountPolicy recoveryDiscountPolicy;

    @InjectMocks
    private CartService cartService;

    @Test
    void createCartInitializesTotalAmountToZero() {
        Customer customer = new Customer();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cart cart = cartService.createCart(1L, null,null);

        assertEquals(BigDecimal.ZERO, cart.getTotalAmount());
    }

    @Test
    void createCartUsesTotalAmountFromRequest() {
        Customer customer = new Customer();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cart cart = cartService.createCart(
                1L,
                new BigDecimal("49.99"),
                new BigDecimal("100.00")
        );

        assertEquals(new BigDecimal("49.99"), cart.getTotalAmount());
    }

    @Test
    void updateCartUpdatesCustomerAndTotalAmount() {
        Customer originalCustomer = new Customer();
        Customer updatedCustomer = new Customer();
        Cart cart = new Cart();
        cart.setCustomer(originalCustomer);
        cart.setTotalAmount(BigDecimal.ZERO);

        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(customerRepository.findById(2L))
                .thenReturn(Optional.of(updatedCustomer));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cart updatedCart = cartService.updateCart(
                10L,
                2L,
                new BigDecimal("25.50"),
                null
        );

        assertEquals(updatedCustomer, updatedCart.getCustomer());
        assertEquals(new BigDecimal("25.50"), updatedCart.getTotalAmount());
        assertEquals(true, updatedCart.getUpdatedAt().isBefore(
                LocalDateTime.now().plusSeconds(1)
        ));
    }

    @Test
    void updateCartItemReplacesProductAndQuantity() {
        Cart cart = new Cart();
        cart.setCartId(1L);
        cart.setCartStatus(CartStatus.ACTIVE);

        Product oldProduct = new Product();
        oldProduct.setProductId(100L);

        Product newProduct = new Product();
        newProduct.setProductId(200L);
        newProduct.setProductName("Updated product");
        newProduct.setPrice(new BigDecimal("12.00"));
        newProduct.setStockQuantity(10);

        CartItem cartItem = new CartItem();
        cartItem.setCartItemId(10L);
        cartItem.setCart(cart);
        cartItem.setProduct(oldProduct);
        cartItem.setQuantity(1);
        cartItem.setUnitPrice(new BigDecimal("5.00"));

        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(10L))
                .thenReturn(Optional.of(cartItem));
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(newProduct));
        when(cartItemRepository
                .findByCartCartIdAndProductProductId(1L, 200L))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findByCartCartId(1L))
                .thenReturn(List.of(cartItem));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CartItem updatedItem = cartService.updateCartItem(
                1L,
                10L,
                200L,
                3
        );

        assertEquals(newProduct, updatedItem.getProduct());
        assertEquals(3, updatedItem.getQuantity());
        assertEquals(new BigDecimal("12.00"), updatedItem.getUnitPrice());
        assertEquals(new BigDecimal("36.00"), cart.getTotalAmount());
    }

    @Test
    void checkoutRecoversAbandonedCartAndUpdatesAbandonmentRecord() {
        Cart cart = new Cart();
        cart.setCartId(2L);
        cart.setCartStatus(CartStatus.REMINDER_SENT);

        Product product = new Product();
        product.setProductName("Recovered product");
        product.setStockQuantity(5);

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);

        when(cartRepository.findById(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(2L))
                .thenReturn(List.of(cartItem));
        when(cartRepository.save(cart)).thenReturn(cart);

        Cart checkedOutCart = cartService.checkout(2L);

        assertEquals(CartStatus.RECOVERED, checkedOutCart.getCartStatus());
        assertNotNull(checkedOutCart.getRecoveredAt());
        assertEquals(4, product.getStockQuantity());
        verify(abandonedCartService).createForCart(2L);
        verify(abandonedCartService).markRecovered(2L);
    }

    @Test
    void checkoutMarksActiveCartCheckedOut() {
        Cart cart = new Cart();
        cart.setCartId(3L);
        cart.setCartStatus(CartStatus.ACTIVE);

        Product product = new Product();
        product.setProductName("Product");
        product.setStockQuantity(2);

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);

        when(cartRepository.findById(3L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(3L))
                .thenReturn(List.of(cartItem));
        when(cartRepository.save(cart)).thenReturn(cart);

        Cart checkedOutCart = cartService.checkout(3L);

        assertEquals(
                CartStatus.CHECKED_OUT,
                checkedOutCart.getCartStatus()
        );
        verify(abandonedCartService, never()).markRecovered(3L);
    }

    @Test
    void checkoutAppliesValidTenPercentRecoveryDiscount() {
        Cart cart = new Cart();
        cart.setCartId(6L);
        cart.setCartStatus(CartStatus.REMINDER_SENT);
        cart.setTotalAmount(new BigDecimal("2000.00"));

        Product product = new Product();
        product.setProductName("Product");
        product.setStockQuantity(2);

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);

        AbandonedCart abandonedCart = new AbandonedCart();
        abandonedCart.setCart(cart);
        abandonedCart.setDiscountCode("RECOVER-6-TEST");
        abandonedCart.setDiscountPercentage(10);

        when(cartRepository.findById(6L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(6L))
                .thenReturn(List.of(cartItem));
        when(abandonedCartService.getByCartId(6L))
                .thenReturn(abandonedCart);
        when(recoveryDiscountPolicy.apply(new BigDecimal("2000.00"), 10))
                .thenReturn(new BigDecimal("1800.00"));
        when(cartRepository.save(cart)).thenReturn(cart);

        Cart checkedOutCart = cartService.checkout(6L, "RECOVER-6-TEST");

        assertEquals(new BigDecimal("1800.00"), checkedOutCart.getTotalAmount());
        assertEquals(CartStatus.RECOVERED, checkedOutCart.getCartStatus());
    }

    @Test
    void checkoutRejectsExpiredCart() {
        Cart cart = new Cart();
        cart.setCartId(4L);
        cart.setCartStatus(CartStatus.EXPIRED);

        when(cartRepository.findById(4L)).thenReturn(Optional.of(cart));

        assertThrows(
                IllegalStateException.class,
                () -> cartService.checkout(4L)
        );
        verify(cartItemRepository, never()).findByCartCartId(4L);
    }

    @Test
    void addingProductAfterFirstReminderResetsRecoveryToPending() {
        Cart cart = new Cart();
        cart.setCartId(5L);
        cart.setCartStatus(CartStatus.REMINDER_SENT);
        cart.setReminderSent(true);

        Product product = new Product();
        product.setProductId(50L);
        product.setProductName("Another product");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(5);

        when(cartRepository.findById(5L))
                .thenReturn(Optional.of(cart));
        when(productRepository.findById(50L))
                .thenReturn(Optional.of(product));
        when(cartItemRepository
                .findByCartCartIdAndProductProductId(5L, 50L))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findByCartCartId(5L))
                .thenAnswer(invocation -> List.of(
                        newCartItem(cart, product, 1)
                ));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        cartService.addItemToCart(5L, 50L, 1);

        assertEquals(CartStatus.ACTIVE, cart.getCartStatus());
        assertFalse(cart.getReminderSent());
        verify(abandonedCartService).resetForCustomerActivity(5L);
    }

    private CartItem newCartItem(
            Cart cart,
            Product product,
            int quantity
    ) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setUnitPrice(product.getPrice());
        return cartItem;
    }
}
