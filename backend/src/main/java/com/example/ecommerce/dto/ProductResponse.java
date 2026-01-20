package com.example.ecommerce.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductResponse {
    String id;
    String name;
    String description;
    BigDecimal price;
    int stock;
}
