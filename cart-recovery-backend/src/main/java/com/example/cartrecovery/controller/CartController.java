package com.example.cartrecovery.controller;

import com.example.cartrecovery.dto.CartRequest;
import com.example.cartrecovery.dto.CartResponse;
import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.service.AbandonedCartService;
import com.example.cartrecovery.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;
    private final AbandonedCartService  abandonedCartService;

    public CartController(CartService cartService,
                          AbandonedCartService abandonedCartService) {
        this.cartService = cartService;
        this.abandonedCartService = abandonedCartService;
    }

    @PostMapping
    public ResponseEntity<CartResponse> createCart(
            @Valid @RequestBody CartRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(
                        cartService.createCart(
                                request.getCustomerId(),
                                request.getTotalAmount(),
                                request.getSubTotal()

                        )
                ));
    }


    @GetMapping("/{cartId}")
    public CartResponse getCart(@PathVariable Long cartId) {
        return toResponse(cartService.getCartById(cartId));
    }

    @GetMapping
    public List<CartResponse> getCarts() {
        return cartService.getAllCarts()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/customer/{customerId}")
    public List<CartResponse> getCustomerCarts(
            @PathVariable Long customerId) {
        return cartService.getCartsByCustomer(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{cartId}")
    public CartResponse updateCart(
            @PathVariable Long cartId,
            @Valid @RequestBody CartRequest request) {
        return toResponse(cartService.updateCart(
                cartId,
                request.getCustomerId(),
                request.getTotalAmount(),
                request.getDiscountAmount()
        ));
    }

    @PostMapping("/{cartId}/checkout")
    public CartResponse checkout(
            @PathVariable Long cartId,
            @RequestParam(required = false) String discountCode
    ) {
        return toResponse(cartService.checkout(cartId, discountCode));
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long cartId) {
        cartService.deleteCart(cartId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{cartId}/validate-discount")
    public ResponseEntity<?> validateDiscount(@PathVariable Long cartId, @RequestParam String code) {
        AbandonedCart abandonedCart=abandonedCartService.getByCartId(cartId);
        if(abandonedCart==null ||
        abandonedCart.getDiscountCode()==null){
            return ResponseEntity.ok(
                    Map.of("valid", false)
            );}
            boolean valid= abandonedCart.getDiscountCode().equalsIgnoreCase(code);

            if(!valid){
                return ResponseEntity.ok(Map.of("valid", false));
            }

return  ResponseEntity.ok(Map.of("valid", true,
         "discountCode", abandonedCart.getDiscountCode(),
        "discountPercentage", abandonedCart.getDiscountPercentage()
        
        ));
    }

    private CartResponse toResponse(Cart cart) {
        Long customerId = cart.getCustomer() == null
                ? null
                : cart.getCustomer().getCustomerId();
        String status = cart.getCartStatus() == null
                ? null
                : cart.getCartStatus().name();

        return new CartResponse(
                cart.getCartId(),
                customerId,
                status,
                cart.getTotalAmount(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                cart.getAbandonedAt(),
                cart.getRecoveredAt(),
                cart.getReminderSent(),
                cart.getSubTotal()
        );
    }
}
