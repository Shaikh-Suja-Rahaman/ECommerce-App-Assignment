package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentCreateRequest;
import com.example.ecommerce.dto.PaymentResponse;
import com.example.ecommerce.dto.PaymentWebhookRequest;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderStatus;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.PaymentStatus;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ObjectProvider<RazorpayClient> razorpayClientProvider;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    @Value("${razorpay.webhookSecret:}")
    private String webhookSecret;

    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not in CREATED state");
        }

        Optional<Payment> existing = paymentRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        BigDecimal amount = order.getTotalAmount();
        RazorpayClient razorpayClient = razorpayClientProvider.getIfAvailable();
        if (razorpayClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Razorpay credentials are not configured");
        }

        try {
            JSONObject options = new JSONObject();
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", order.getId());
            options.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);
            String razorpayOrderId = razorpayOrder.get("id");

                    Payment payment = Payment.builder()
                        .orderId(order.getId())
                        .amount(amount)
                        .status(PaymentStatus.PENDING)
                        .providerOrderId(razorpayOrderId)
                        .createdAt(java.time.Instant.now())
                        .build();
            Payment saved = paymentRepository.save(payment);
            return toResponse(saved);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create Razorpay order: " + e.getMessage());
        }
    }

    public void handleWebhook(String payload, String signature) {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            try {
                Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
            }
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            PaymentWebhookRequest webhook = objectMapper.readValue(payload, PaymentWebhookRequest.class);
            if (webhook.getPayload() == null || webhook.getPayload().getPayment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload");
            }
            PaymentWebhookRequest.Payment paymentEntity = webhook.getPayload().getPayment();

            String razorpayPaymentId = paymentEntity.getId();
            String razorpayOrderId = paymentEntity.getOrderId();
            String status = paymentEntity.getStatus();

            Payment payment = paymentRepository.findByProviderOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found for webhook"));

            PaymentStatus newStatus = "captured".equalsIgnoreCase(status) || "authorized".equalsIgnoreCase(status)
                    ? PaymentStatus.SUCCESS
                    : PaymentStatus.FAILED;

            payment.setStatus(newStatus);
            payment.setPaymentId(razorpayPaymentId);
            paymentRepository.save(payment);

            if (newStatus == PaymentStatus.SUCCESS) {
                orderService.markOrderPaid(payment.getOrderId());
            } else {
                orderService.markOrderFailed(payment.getOrderId());
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse webhook: " + e.getMessage());
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
            .paymentId(payment.getPaymentId())
            .providerOrderId(payment.getProviderOrderId())
                .build();
    }
}
