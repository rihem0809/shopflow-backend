// com.shopflow.dto.response.TopSellerResponse.java
package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSellerResponse {
    private Long id;
    private String shopName;
    private String sellerName;
    private Double totalRevenue;
    private Long productCount;
    private Long orderCount;
    private Double averageRating;
}