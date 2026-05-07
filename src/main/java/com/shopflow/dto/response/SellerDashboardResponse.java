package com.shopflow.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardResponse {
    private Double totalRevenue;
    private Long totalOrders;
    private Long pendingOrders;
    private List<ProductResponse> lowStockProducts;
    private List<OrderResponse> recentOrders;
}