package com.example.ecommerce.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentResponse {
    String id;
    String orderId;
    BigDecimal amount;
    String status;
    String paymentId;
    String providerOrderId;
}
