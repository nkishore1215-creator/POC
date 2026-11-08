package com.example.cartrecovery.repository;

import com.example.cartrecovery.entity.CartItem;
import com.example.cartrecovery.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartCartId(Long cartId);

    List<CartItem> findByProductProductId(Long productId);

    Optional<CartItem> findByCartCartIdAndProductProductId(Long cartId, Long productId);

    void deleteByCartCartId(Long cartId);

    long countByCartCartId(Long cartId);

    @Query("""
            SELECT COUNT(DISTINCT item.cart.customer.customerId)
            FROM CartItem item
            WHERE item.product.productId = :productId
              AND item.cart.cartStatus IN :completedStatuses
              AND item.cart.updatedAt >= :purchasedSince
            """)
    long countCustomersWhoPurchasedProductSince(
            @Param("productId") Long productId,
            @Param("completedStatuses") List<CartStatus> completedStatuses,
            @Param("purchasedSince") LocalDateTime purchasedSince
    );

    @Query("""
            SELECT COALESCE(SUM(item.quantity), 0)
            FROM CartItem item
            WHERE item.product.productId = :productId
              AND item.cart.cartStatus IN :completedStatuses
              AND item.cart.updatedAt >= :purchasedSince
            """)
    long countPurchasedUnitsForProductSince(
            @Param("productId") Long productId,
            @Param("completedStatuses") List<CartStatus> completedStatuses,
            @Param("purchasedSince") LocalDateTime purchasedSince
    );
}
