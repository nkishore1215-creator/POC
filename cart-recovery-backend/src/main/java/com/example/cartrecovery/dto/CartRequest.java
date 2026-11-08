package com.example.cartrecovery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartRequest {

    @NotNull(message = "Customer ID is required.")
    @Positive(message = "Customer ID must be greater than zero.")
    private Long customerId;

    @DecimalMin(value = "0.00", message = "Total amount cannot be negative.")
    private BigDecimal totalAmount;

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    @DecimalMin(value = "0.00" , message = "Discount amount cannot be negative")
    private BigDecimal discountAmount;

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

    @DecimalMin(value = "0.00", message = "Total amount cannot be negative.")
    private BigDecimal subTotal;

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public CartRequest() {
    }

    public CartRequest(Long customerId) {
        this.customerId = customerId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
}
