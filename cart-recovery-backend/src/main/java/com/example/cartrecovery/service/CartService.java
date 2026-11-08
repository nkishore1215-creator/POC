package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartItem;
import com.example.cartrecovery.entity.CartStatus;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.entity.Product;
import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.exception.CartItemNotFoundException;
import com.example.cartrecovery.exception.CartNotFoundException;
import com.example.cartrecovery.exception.CustomerNotFoundException;
import com.example.cartrecovery.exception.ProductNotFoundException;
import com.example.cartrecovery.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartService {


    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AbandonedCartService abandonedCartService;
    private final RecoveryDiscountPolicy recoveryDiscountPolicy;
    private final AbandonedCartRepository abandonedCartRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            AbandonedCartService abandonedCartService,
            RecoveryDiscountPolicy recoveryDiscountPolicy,
    AbandonedCartRepository abandonedRepository) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.abandonedCartService = abandonedCartService;
        this.recoveryDiscountPolicy = recoveryDiscountPolicy;
        this.abandonedCartRepository = abandonedRepository;
    }

    /**
     * Create a new cart for a customer.
     */
    public Cart createCart(Long customerId, BigDecimal totalAmount, BigDecimal subTotal) {

        if (totalAmount != null && totalAmount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Total amount cannot be negative."
            );
        }if (subTotal != null && subTotal.signum() < 0) {
            throw new IllegalArgumentException(
                    "Total amount cannot be negative."
            );
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        Cart cart = new Cart();

        cart.setCustomer(customer);
        cart.setCartStatus(CartStatus.ACTIVE);
        cart.setTotalAmount(
                totalAmount == null ? BigDecimal.ZERO : totalAmount
        );
log.info("set total amount"+totalAmount);
        cart.setSubTotal(
  subTotal==null ? BigDecimal.ZERO : subTotal
        );
        log.info("set sub total amount"+subTotal);
        LocalDateTime createdAt = LocalDateTime.now();
        cart.setCreatedAt(createdAt);
        cart.setUpdatedAt(createdAt);

        cart.setReminderSent(false);

        return cartRepository.save(cart);
    }

    /**
     * Get a cart by ID.
     */
    public Cart getCartById(Long cartId) {

        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
    }

    /**
     * Get all carts.
     */
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    /**
     * Get all carts for a customer.
     */
    public List<Cart> getCartsByCustomer(Long customerId) {

        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }

        return cartRepository.findByCustomerCustomerId(customerId);
    }

    /**
     * Update a cart's customer and, when supplied, its total amount.
     */
    @Transactional
    public Cart updateCart(
            Long cartId,
            Long customerId,
            BigDecimal totalAmount,
            BigDecimal discountAmount) {

        Cart cart = getCartById(cartId);
        ensureCartEditable(cart);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (totalAmount != null && totalAmount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Total amount cannot be negative."
            );
        }
        if(discountAmount != null && discountAmount.signum() < 0) {
            throw new IllegalArgumentException("discount amount cannot be negative.");
        }

        cart.setCustomer(customer);

        if (totalAmount != null) {
            cart.setTotalAmount(totalAmount);
        }

        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart= cartRepository.save(cart);

       if(discountAmount != null) {
           try{
               AbandonedCart abandonedCart=abandonedCartService.getByCartId(cartId);
               abandonedCart.setDiscountAmount(discountAmount);
               abandonedCartRepository.save(abandonedCart);

           }catch (Exception e){
               log.error(e.getMessage());
           }
       }

return savedCart;
    }

    /**
     * Add a product to a cart.
     */
    @Transactional
    public CartItem addItemToCart(
            Long cartId,
            Long productId,
            Integer quantity) {

        validateQuantity(quantity);

        Cart cart = getCartById(cartId);
        ensureCartEditable(cart);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: "
                            + product.getProductName()
            );
        }

        CartItem existingItem = cartItemRepository
                .findByCartCartIdAndProductProductId(cartId, productId)
                .orElse(null);

        CartItem savedItem;

        if (existingItem != null) {
            existingItem.setQuantity(
                    existingItem.getQuantity() + quantity
            );

            savedItem = cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setCreatedAt(LocalDateTime.now());
            cartItem.setUnitPrice(product.getPrice());

            savedItem = cartItemRepository.save(cartItem);
        }

        reactivateCart(cart);
        recalculateCartTotal(cartId);

        return savedItem;
    }

    /**
     * Get all items from a cart.
     */
    public List<CartItem> getCartItems(Long cartId) {

        getCartById(cartId);

        return cartItemRepository.findByCartCartId(cartId);
    }

    /**
     * Replace the editable fields of an existing cart-item record.
     */
    @Transactional
    public CartItem updateCartItem(
            Long cartId,
            Long cartItemId,
            Long productId,
            Integer quantity) {

        validateQuantity(quantity);

        Cart cart = getCartById(cartId);
        ensureCartEditable(cart);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));

        if (!cartItem.getCart().getCartId().equals(cartId)) {
            throw new IllegalArgumentException(
                    "Cart item does not belong to cart ID: " + cartId
            );
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: "
                            + product.getProductName()
            );
        }

        CartItem itemWithProduct = cartItemRepository
                .findByCartCartIdAndProductProductId(cartId, productId)
                .orElse(null);

        if (itemWithProduct != null
                && !itemWithProduct.getCartItemId().equals(cartItemId)) {
            throw new IllegalStateException(
                    "Product ID " + productId
                            + " already exists in cart ID: " + cartId
            );
        }

        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setCreatedAt(LocalDateTime.now());
        cartItem.setUnitPrice(product.getPrice());

        CartItem updatedItem = cartItemRepository.save(cartItem);

        reactivateCart(cart);
        recalculateCartTotal(cartId);

        return updatedItem;
    }

    /**
     * Update the quantity of a cart item.
     */
    @Transactional
    public CartItem updateCartItemQuantity(
            Long cartId,
            Long cartItemId,
            Integer quantity) {

        validateQuantity(quantity);

        Cart cart = getCartById(cartId);
        ensureCartEditable(cart);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));

        if (!cartItem.getCart().getCartId().equals(cartId)) {
            throw new IllegalArgumentException(
                    "Cart item does not belong to cart ID: " + cartId
            );
        }

        Product product = cartItem.getProduct();

        if (product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: "
                            + product.getProductName()
            );
        }

        cartItem.setQuantity(quantity);

        CartItem updatedItem = cartItemRepository.save(cartItem);

        reactivateCart(cart);
        recalculateCartTotal(cartId);

        return updatedItem;
    }

    /**
     * Remove one item from a cart.
     */
    @Transactional
    public void removeItemFromCart(
            Long cartId,
            Long cartItemId) {

        Cart cart = getCartById(cartId);
        ensureCartEditable(cart);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));

        if (!cartItem.getCart().getCartId().equals(cartId)) {
            throw new IllegalArgumentException(
                    "Cart item does not belong to cart ID: " + cartId
            );
        }

        cartItemRepository.delete(cartItem);

        reactivateCart(cart);
        recalculateCartTotal(cartId);
    }

    /**
     * Remove all items from a cart.
     */
    @Transactional
    public void clearCart(Long cartId) {

        Cart cart = getCartById(cartId);
        ensureCartEditable(cart);

        cartItemRepository.deleteByCartCartId(cartId);

        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setCartStatus(CartStatus.ACTIVE);
        cart.setUpdatedAt(LocalDateTime.now());
        cart.setReminderSent(false);

        cartRepository.save(cart);
    }

    /**
     * Complete checkout.
     */
    @Transactional
    public Cart checkout(Long cartId) {
        return checkout(cartId, null);
    }

    @Transactional
    public Cart checkout(Long cartId, String discountCode) {

        Cart cart = getCartById(cartId);

        if (isTerminal(cart.getCartStatus())) {
            throw new IllegalStateException(
                    "Cart has already been checked out or expired."
            );
        }

        List<CartItem> cartItems =
                cartItemRepository.findByCartCartId(cartId);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot checkout an empty cart."
            );
        }

        for (CartItem cartItem : cartItems) {

            Product product = cartItem.getProduct();
            int remainingStock =
                    product.getStockQuantity() - cartItem.getQuantity();

            if (remainingStock < 0) {
                throw new IllegalStateException(
                        "Insufficient stock for product: "
                                + product.getProductName()
                );
            }

            product.setStockQuantity(remainingStock);
            productRepository.save(product);
        }

        boolean abandonedCart =
                cart.getCartStatus() == CartStatus.ABANDONED ||
                        cart.getCartStatus() == CartStatus.REMINDER_SENT;

        if (discountCode != null && !discountCode.isBlank()) {
            applyRecoveryDiscount(cart, abandonedCart, discountCode);
        }

        if (abandonedCart) {
            cart.setCartStatus(CartStatus.RECOVERED);
            cart.setRecoveredAt(LocalDateTime.now());
        } else {
            cart.setCartStatus(CartStatus.CHECKED_OUT);
        }

        cart.setUpdatedAt(LocalDateTime.now());

        Cart checkedOutCart = cartRepository.save(cart);

        if (abandonedCart) {
            abandonedCartService.createForCart(cartId);
            abandonedCartService.markRecovered(cartId);
        }

        return checkedOutCart;
    }

    private void applyRecoveryDiscount(
            Cart cart,
            boolean recoveryCart,
            String suppliedCode
    ) {
        if (!recoveryCart) {
            throw new IllegalArgumentException(
                    "Recovery discount codes apply only to abandoned carts."
            );
        }

        AbandonedCart abandonedCart = abandonedCartService.getByCartId(
                cart.getCartId()
        );
        String expectedCode = abandonedCart.getDiscountCode();
        Integer percentage = abandonedCart.getDiscountPercentage();

        if (expectedCode == null
                || !expectedCode.equalsIgnoreCase(suppliedCode.trim())
                || percentage == null
                || percentage <= 0) {
            throw new IllegalArgumentException("Invalid recovery discount code.");
        }

        cart.setTotalAmount(recoveryDiscountPolicy.apply(
                cart.getTotalAmount(),
                percentage
        ));
    }

    /**
     * Delete a cart.
     */
    @Transactional
    public void deleteCart(Long cartId) {

        Cart cart = getCartById(cartId);

        cartItemRepository.deleteByCartCartId(cartId);
        cartRepository.delete(cart);
    }

    /**
     * Recalculate the cart total from its items.
     */
    private void recalculateCartTotal(Long cartId) {

        Cart cart = getCartById(cartId);

        List<CartItem> cartItems =
                cartItemRepository.findByCartCartId(cartId);

        BigDecimal subTotal = cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalAmount(subTotal);
        cart.setSubTotal(subTotal);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
        abandonedCartService.resetForCustomerActivity(cart.getCartId());
    }

    /**
     * Make an abandoned cart active when the customer updates it.
     */
    private void reactivateCart(Cart cart) {

        cart.setCartStatus(CartStatus.ACTIVE);
        cart.setAbandonedAt(null);
        cart.setReminderSent(false);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
    }

    private void validateQuantity(Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }
    }

    private void ensureCartEditable(Cart cart) {
        if (isTerminal(cart.getCartStatus())) {
            throw new IllegalStateException(
                    "A checked-out, recovered, or expired cart cannot be modified."
            );
        }
    }

    private boolean isTerminal(CartStatus status) {
        return status == CartStatus.CHECKED_OUT
                || status == CartStatus.COMPLETED
                || status == CartStatus.RECOVERED
                || status == CartStatus.EXPIRED;
    }
}
