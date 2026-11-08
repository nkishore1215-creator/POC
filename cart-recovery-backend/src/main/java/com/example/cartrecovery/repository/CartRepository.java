package com.example.cartrecovery.repository;

import com.example.cartrecovery.entity.Cart;
import com.example.cartrecovery.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByCustomerCustomerId(Long customerId);


    List<Cart> findByCartStatus(CartStatus cartStatus);


    List<Cart> findByCartStatusAndReminderSentFalse(
            CartStatus cartStatus);





    /**
     * Finds carts that are still active and whose last activity occurred on or
     * before the supplied cutoff. Newly created carts fall back to createdAt
     * when updatedAt has not been populated yet.
     */
    @Query("""
            SELECT cart
            FROM Cart cart
            WHERE cart.cartStatus = :cartStatus
              AND COALESCE(cart.updatedAt, cart.createdAt) <= :cutoff
            """)
    List<Cart> findIdleCarts(
            @Param("cartStatus") CartStatus cartStatus,
            @Param("cutoff") LocalDateTime cutoff
    );

    List<Cart> findByReminderSentTrue();


    List<Cart> findByUpdatedAtAfter(LocalDateTime dateTime);


}
