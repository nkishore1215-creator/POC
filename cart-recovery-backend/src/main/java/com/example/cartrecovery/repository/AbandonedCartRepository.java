package com.example.cartrecovery.repository;

import com.example.cartrecovery.entity.AbandonedCart;
import com.example.cartrecovery.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AbandonedCartRepository
        extends JpaRepository<AbandonedCart, Long> {


    Optional<AbandonedCart> findByCartCartId(Long cartId);


    boolean existsByCartCartId(Long cartId);


    List<AbandonedCart> findByCustomerCustomerId(Long customerId);


    List<AbandonedCart> findByRecoveryStatus(String recoveryStatus);


    List<AbandonedCart> findByReminderSentFalse();


    List<AbandonedCart> findByReminderSentTrue();

    List<AbandonedCart> findByReminderSentFalseAndAbandonedAtLessThanEqualAndRecoveryStatusNot(
            LocalDateTime abandonedBefore,
            String excludedRecoveryStatus
    );

    List<AbandonedCart> findByReminderSentTrueAndReminderSentAtLessThanEqualAndRecoveryStatusNot(
            @Param("reminderSentBefore") LocalDateTime reminderSentBefore,
            @Param("excludedRecoveryStatus") String excludedRecoveryStatus
    );

    @Query("""
            SELECT abandonedCart
            FROM AbandonedCart abandonedCart
            WHERE abandonedCart.recoveryStatus <> :excludedRecoveryStatus
              AND abandonedCart.cart.cartStatus = :cartStatus
              AND EXISTS (
                  SELECT notification.notificationId
                  FROM Notification notification
                  WHERE notification.cart.cartId =
                        abandonedCart.cart.cartId
                    AND notification.notificationType = :notificationType
                    AND notification.notificationStatus = :notificationStatus
                    AND notification.sentAt <= :sentBefore
              )
            """)
    List<AbandonedCart> findDueAfterSuccessfulNotification(
            @Param("sentBefore") LocalDateTime sentBefore,
            @Param("notificationType") String notificationType,
            @Param("notificationStatus") String notificationStatus,
            @Param("cartStatus") CartStatus cartStatus,
            @Param("excludedRecoveryStatus") String excludedRecoveryStatus
    );

}