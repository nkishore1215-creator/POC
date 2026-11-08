package com.example.cartrecovery.controller;

import com.example.cartrecovery.dto.CartItemRequest;
import com.example.cartrecovery.entity.CartItem;
import com.example.cartrecovery.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/carts/{cartId}/items")
public class CartItemController {

    private final CartService cartService;

    public CartItemController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartItem> addCartItem(
            @PathVariable Long cartId,
            @Valid @RequestBody CartItemRequest request) {
        CartItem cartItem = cartService.addItemToCart(
                cartId,
                request.getProductId(),
                request.getQuantity()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartItem);
    }

    @GetMapping
    public List<CartItem> getCartItems(@PathVariable Long cartId) {
        return cartService.getCartItems(cartId);
    }

    @PutMapping("/{cartItemId}")
    public CartItem updateCartItem(
            @PathVariable Long cartId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemRequest request) {
        return cartService.updateCartItem(
                cartId,
                cartItemId,
                request.getProductId(),
                request.getQuantity()
        );
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable Long cartId,
            @PathVariable Long cartItemId) {
        cartService.removeItemFromCart(cartId, cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@PathVariable Long cartId) {
        cartService.clearCart(cartId);
        return ResponseEntity.noContent().build();
    }
}
