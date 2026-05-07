// com.shopflow.dto.response.CategorySalesResponse.java
package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySalesResponse {
    private String categoryName;
    private Double sales;
    private Long productCount;
}