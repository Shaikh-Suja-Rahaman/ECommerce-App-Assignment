package com.example.ecommerce.model;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    private String orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentId; // External payment id (after capture)
    private String providerOrderId; // Razorpay order id created during initiation
    private Instant createdAt;
}
