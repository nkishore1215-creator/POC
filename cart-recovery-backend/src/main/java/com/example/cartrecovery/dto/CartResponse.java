package com.example.cartrecovery.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CartResponse {

    private Long cartId;
    private Long customerId;
    private String cartStatus;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime abandonedAt;
    private LocalDateTime recoveredAt;
    private Boolean reminderSent;
    private  BigDecimal  subTotal;

    public CartResponse() {
    }

    public CartResponse(
            Long cartId,
            Long customerId,
            String cartStatus,
            BigDecimal totalAmount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime abandonedAt,
            LocalDateTime recoveredAt,
            Boolean reminderSent,
    BigDecimal subTotal) {
        this.cartId = cartId;
        this.customerId = customerId;
        this.cartStatus = cartStatus;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.abandonedAt = abandonedAt;
        this.recoveredAt = recoveredAt;
        this.reminderSent = reminderSent;
        this.subTotal = subTotal;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCartStatus() {
        return cartStatus;
    }

    public void setCartStatus(String cartStatus) {
        this.cartStatus = cartStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getAbandonedAt() {
        return abandonedAt;
    }

    public void setAbandonedAt(LocalDateTime abandonedAt) {
        this.abandonedAt = abandonedAt;
    }

    public LocalDateTime getRecoveredAt() {
        return recoveredAt;
    }

    public void setRecoveredAt(LocalDateTime recoveredAt) {
        this.recoveredAt = recoveredAt;
    }

    public Boolean getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}
