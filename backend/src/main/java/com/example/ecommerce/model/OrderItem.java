package com.example.ecommerce.model;

import java.math.BigDecimal;
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
@Document(collection = "order_items")
public class OrderItem {
    @Id
    private String id;
    private String orderId;
    private String productId;
    private int quantity;
    private BigDecimal price;
}
