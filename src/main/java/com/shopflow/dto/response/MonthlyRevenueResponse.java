// com.shopflow.dto.response.MonthlyRevenueResponse.java
package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueResponse {
    private String month;
    private Double revenue;
    private Long orderCount;
}