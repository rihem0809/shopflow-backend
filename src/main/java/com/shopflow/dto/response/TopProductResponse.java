// com.shopflow.dto.response.TopProductResponse.java
package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {
    private Long id;
    private String name;
    private String image;
    private Long totalSold;
    private Double revenue;
    private Double averageRating;
}