package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddToCartRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartItemResponse addToCart(AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getStock() < request.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock");
        }

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(request.getUserId(), request.getProductId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + request.getQuantity());
                    return existing;
                })
                .orElse(CartItem.builder()
                        .userId(request.getUserId())
                        .productId(request.getProductId())
                        .quantity(request.getQuantity())
                        .build());

        CartItem saved = cartItemRepository.save(cartItem);
        return toResponse(saved, product);
    }

    public List<CartItemResponse> getCart(String userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return items.stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
                    return toResponse(item, product);
                })
                .toList();
    }

    public void clearCart(String userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private CartItemResponse toResponse(CartItem item, Product product) {
        return CartItemResponse.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .product(CartItemResponse.ProductSummary.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .price(product.getPrice())
                        .build())
                .build();
    }
}
