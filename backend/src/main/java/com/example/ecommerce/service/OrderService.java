package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderItemResponse;
import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.OrderStatus;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    public OrderResponse createFromCart(String userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Map<String, Integer> requestedQuantities = cartItems.stream()
                .collect(Collectors.toMap(CartItem::getProductId, CartItem::getQuantity));

        List<com.example.ecommerce.model.Product> products = java.util.stream.StreamSupport
                .stream(productRepository.findAllById(requestedQuantities.keySet()).spliterator(), false)
                .toList();
        Map<String, com.example.ecommerce.model.Product> productMap = products.stream()
                .collect(Collectors.toMap(com.example.ecommerce.model.Product::getId, Function.identity()));

        // Validate stock
        for (CartItem item : cartItems) {
            com.example.ecommerce.model.Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in catalog");
            }
            if (product.getStock() < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + product.getName());
            }
        }

        BigDecimal total = cartItems.stream()
                .map(item -> productMap.get(item.getProductId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(total)
                .status(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .build();
        Order savedOrder = orderRepository.save(order);

        // Create order items and update stock
        for (CartItem item : cartItems) {
            com.example.ecommerce.model.Product product = productMap.get(item.getProductId());
            OrderItem orderItem = OrderItem.builder()
                    .orderId(savedOrder.getId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .price(product.getPrice())
                    .build();
            orderItemRepository.save(orderItem);

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        cartItemRepository.deleteByUserId(userId);

        return getOrderResponse(savedOrder.getId());
    }

    public OrderResponse getOrderResponse(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        List<OrderItemResponse> items = orderItems.stream()
                .map(oi -> OrderItemResponse.builder()
                        .productId(oi.getProductId())
                        .quantity(oi.getQuantity())
                        .price(oi.getPrice())
                        .build())
                .toList();

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .items(items)
                .payment(payment == null ? null : OrderResponse.PaymentSummary.builder()
                        .id(payment.getId())
                        .status(payment.getStatus().name())
                        .amount(payment.getAmount())
                        .paymentId(payment.getPaymentId())
                        .build())
                .build();
    }

    public List<OrderResponse> listOrdersForUser(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(order -> getOrderResponse(order.getId()))
                .toList();
    }

    public void markOrderPaid(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    public void markOrderFailed(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
    }
}
