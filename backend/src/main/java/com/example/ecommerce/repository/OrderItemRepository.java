package com.example.ecommerce.repository;

import com.example.ecommerce.model.OrderItem;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderItemRepository extends MongoRepository<OrderItem, String> {
    List<OrderItem> findByOrderId(String orderId);
}
