package com.example.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymentWebhookRequest {
    private String event;
    private Payload payload;

    @Data
    public static class Payload {
        private Payment payment;
    }

    @Data
    public static class Payment {
        private String id;
        @JsonProperty("order_id")
        private String orderId;
        private String status;
    }
}
