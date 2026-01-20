package com.example.ecommerce.config;

import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RazorpayConfig {

    @Value("${razorpay.keyId:}")
    private String keyId;

    @Value("${razorpay.keySecret:}")
    private String keySecret;

    @Bean
    @ConditionalOnProperty(name = {"razorpay.keyId", "razorpay.keySecret"})
    public RazorpayClient razorpayClient() throws Exception {
        return new RazorpayClient(keyId, keySecret);
    }
}
