package com.example.ecommerce.repository;

import com.example.ecommerce.model.Payment;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
}
