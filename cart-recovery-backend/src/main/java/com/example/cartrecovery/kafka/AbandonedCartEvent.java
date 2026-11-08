package com.example.cartrecovery.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbandonedCartEvent {


    Long cartId;
    Long customerId;
    String customerName;
    String customerPhone;
    String customerEmail;
    BigDecimal totalAmount;
    BigDecimal discountAmount;
    String eventType;
}
