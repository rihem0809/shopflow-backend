package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private Long id;
    private String attribute;
    private String value;
    private Integer extraStock;
    private Double priceDelta;
    private Double finalPrice;  // Prix de base + delta
}