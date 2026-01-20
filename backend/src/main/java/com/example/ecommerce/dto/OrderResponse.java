package com.example.ecommerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderResponse {
    String id;
    String userId;
    BigDecimal totalAmount;
    String status;
    Instant createdAt;
    List<OrderItemResponse> items;
    PaymentSummary payment;

    @Value
    @Builder
    public static class PaymentSummary {
        String id;
        String status;
        BigDecimal amount;
        String paymentId;
    }
}
