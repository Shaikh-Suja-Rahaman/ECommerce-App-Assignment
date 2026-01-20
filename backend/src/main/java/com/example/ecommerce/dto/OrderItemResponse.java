package com.example.ecommerce.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderItemResponse {
    String productId;
    int quantity;
    BigDecimal price;
}
