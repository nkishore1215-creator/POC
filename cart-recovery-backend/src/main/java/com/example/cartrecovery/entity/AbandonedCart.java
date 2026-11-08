package com.example.cartrecovery.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "abandoned_cart")
public class AbandonedCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long abandonedCartId;

    @OneToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;


    public NotificationChannel getNotificationChannel() {
        return notificationChannel;
    }

    public void setNotificationChannel(NotificationChannel notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel")
    private NotificationChannel notificationChannel;

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Long getAbandonedCartId() {
        return abandonedCartId;
    }

    public void setAbandonedCartId(Long abandonedCartId) {
        this.abandonedCartId = abandonedCartId;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public LocalDateTime getAbandonedAt() {
        return abandonedAt;
    }

    public void setAbandonedAt(LocalDateTime abandonedAt) {
        this.abandonedAt = abandonedAt;
    }

    public Boolean getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }

    public Integer getReminderCount() {
        return reminderCount;
    }

    public void setReminderCount(Integer reminderCount) {
        this.reminderCount = reminderCount;
    }

    public String getRecoveryStatus() {
        return recoveryStatus;
    }

    public void setRecoveryStatus(String recoveryStatus) {
        this.recoveryStatus = recoveryStatus;
    }

    public LocalDateTime getRecoveredAt() {
        return recoveredAt;
    }

    public void setRecoveredAt(LocalDateTime recoveredAt) {
        this.recoveredAt = recoveredAt;
    }

    public Boolean getRecovered() {
        return recovered;
    }

    public void setRecovered(Boolean recovered) {
        this.recovered = recovered;
    }

    public LocalDateTime getRecoveredTime() {
        return recoveredTime;
    }

    public void setRecoveredTime(LocalDateTime recoveredTime) {
        this.recoveredTime = recoveredTime;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
    }

    public Integer getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Integer discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /*
     * Keep this explicit: the existing PostgreSQL schema names the required
     * column "abandoned_time". Relying on Hibernate's naming strategy maps
     * this property to "abandoned_at" and leaves abandoned_time null.
     */
    @Column(name = "abandoned_time", nullable = false)
    private LocalDateTime abandonedAt;

    private Boolean reminderSent;


    private LocalDateTime reminderSentAt;


    @Column(name = "reminder_count")
    private Integer reminderCount = 0;

    private String recoveryStatus;


    @Column(name = "recovered")
    private Boolean recovered = false;


    @Column(name = "recovered_time")
    private LocalDateTime recoveredTime;

    private LocalDateTime recoveredAt;

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }


    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount =BigDecimal.ZERO;
    private String discountCode;

    @Column(name = "discount_percentage")
    private Integer discountPercentage;
}
