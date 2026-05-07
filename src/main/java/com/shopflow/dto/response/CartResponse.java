package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long id;
    private List<CartItemResponse> items;
    private Double subtotal;
    private Double discountAmount;
    private Double deliveryFees;
    private Double total;
    private String couponCode;
    private Integer itemCount;
    private LocalDateTime lastModified;
}