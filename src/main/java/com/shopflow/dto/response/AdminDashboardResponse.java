// com.shopflow.dto.response.AdminDashboardResponse.java
package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    // KPI Cards
    private Double totalRevenue;
    private Long totalOrders;
    private Long totalCustomers;
    private Long totalProducts;
    private Double averageOrderValue;

    // Top produits
    private List<TopProductResponse> topProducts;

    // Top vendeurs
    private List<TopSellerResponse> topSellers;

    // Commandes récentes
    private List<OrderResponse> recentOrders;

    // Graphiques
    private List<MonthlyRevenueResponse> monthlyRevenue;
    private List<CategorySalesResponse> salesByCategory;
    private Map<String, Long> ordersByStatus;
}