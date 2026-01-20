package com.example.ecommerce.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CartItemResponse {
    String id;
    String userId;
    String productId;
    int quantity;
    ProductSummary product;

    @Value
    @Builder
    public static class ProductSummary {
        String id;
        String name;
        BigDecimal price;
    }
}
