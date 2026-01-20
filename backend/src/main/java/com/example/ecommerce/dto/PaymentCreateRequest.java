package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentCreateRequest {
    @NotBlank
    private String orderId;
}
