package com.shopflow.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double promoPrice;
    private Double effectivePrice;  // Prix effectif (promo si dispo)
    private Integer stock;
    private String image;
    private Boolean active;
    private String sellerName;
    private Long sellerId;
    private String storeName;
    private List<String> categories;
    private List<ProductVariantResponse> variants;
    private Double averageRating;
    private Integer reviewCount;
    private Integer promotionPercentage;  // Pourcentage de remise
    private LocalDateTime createdAt;
}