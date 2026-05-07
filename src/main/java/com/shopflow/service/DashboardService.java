package com.shopflow.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.shopflow.dto.response.*;
import com.shopflow.entity.*;
import com.shopflow.entity.enums.OrderStatus;
import com.shopflow.entity.enums.Role;
import com.shopflow.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final CategoryRepository categoryRepository;
    private final AuthService authService;

    public AdminDashboardResponse getAdminDashboard() {
        log.info("Génération du dashboard ADMIN");

        List<Order> allOrders = orderRepository.findAll();
        List<Order> validOrders = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        // ========== 1. KPI CARDS ==========
        Double totalRevenue = validOrders.stream()
                .mapToDouble(Order::getTotalTtc)
                .sum();

        Long totalOrders = (long) allOrders.size();
        Long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        Long totalProducts = productRepository.count();
        Double averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        log.info("KPI - CA: {}, Commandes: {}, Clients: {}, Produits: {}",
                totalRevenue, totalOrders, totalCustomers, totalProducts);

        // ========== 2. TOP PRODUITS (10) ==========
        List<TopProductResponse> topProducts = new ArrayList<>();
        List<Product> topSellingProducts = productRepository.findTopSellingProducts(PageRequest.of(0, 10));

        for (Product p : topSellingProducts) {
            topProducts.add(TopProductResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .image(p.getImage() != null ? p.getImage() : "https://via.placeholder.com/60x60")
                    .totalSold(calculateTotalSold(p))
                    .revenue(calculateRevenue(p))
                    .averageRating(p.getAverageRating())
                    .build());
        }
        log.info("Top produits: {} produits chargés", topProducts.size());

        // ========== 3. TOP VENDEURS (10) ==========
        List<TopSellerResponse> topSellers = new ArrayList<>();
        List<SellerProfile> allSellers = sellerProfileRepository.findAll();

        for (SellerProfile seller : allSellers) {
            List<Order> sellerOrders = orderRepository.findBySeller(seller);
            Double revenue = sellerOrders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .mapToDouble(Order::getTotalTtc)
                    .sum();
            Long productCount = productRepository.countBySellerProfile(seller);
            Long orderCount = (long) sellerOrders.size();

            topSellers.add(TopSellerResponse.builder()
                    .id(seller.getId())
                    .shopName(seller.getStoreName())
                    .sellerName(seller.getUser().getFirstName() + " " + seller.getUser().getLastName())
                    .totalRevenue(revenue)
                    .productCount(productCount)
                    .orderCount(orderCount)
                    .averageRating(seller.getRating())
                    .build());
        }

        topSellers = topSellers.stream()
                .sorted((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()))
                .limit(10)
                .collect(Collectors.toList());
        log.info("Top vendeurs: {} vendeurs chargés", topSellers.size());

        // ========== 4. COMMANDES RÉCENTES (10) ==========
        List<OrderResponse> recentOrders = allOrders.stream()
                .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                .limit(10)
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
        log.info("Commandes récentes: {} chargées", recentOrders.size());

        // ========== 5. REVENU MENSUEL (6 mois) ==========
        List<MonthlyRevenueResponse> monthlyRevenue = calculateMonthlyRevenue(validOrders);
        log.info("Revenu mensuel: {} mois chargés", monthlyRevenue.size());

        // ========== 6. VENTES PAR CATÉGORIE ==========
        List<CategorySalesResponse> salesByCategory = calculateSalesByCategory();
        log.info("Ventes par catégorie: {} catégories chargées", salesByCategory.size());

        // ========== 7. STATUT DES COMMANDES ==========
        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        ordersByStatus.put("PENDING", 0L);
        ordersByStatus.put("PAID", 0L);
        ordersByStatus.put("PROCESSING", 0L);
        ordersByStatus.put("SHIPPED", 0L);
        ordersByStatus.put("DELIVERED", 0L);
        ordersByStatus.put("CANCELLED", 0L);

        for (Order order : allOrders) {
            String status = order.getStatus().name();
            ordersByStatus.put(status, ordersByStatus.getOrDefault(status, 0L) + 1);
        }
        log.info("Statut commandes: {}", ordersByStatus);

        return AdminDashboardResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .averageOrderValue(averageOrderValue)
                .topProducts(topProducts)
                .topSellers(topSellers)
                .recentOrders(recentOrders)
                .monthlyRevenue(monthlyRevenue)
                .salesByCategory(salesByCategory)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    private List<MonthlyRevenueResponse> calculateMonthlyRevenue(List<Order> orders) {
        List<MonthlyRevenueResponse> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthLabel = monthDate.format(formatter);

            Double revenue = orders.stream()
                    .filter(o -> o.getOrderDate().getYear() == monthDate.getYear() &&
                            o.getOrderDate().getMonthValue() == monthDate.getMonthValue())
                    .mapToDouble(Order::getTotalTtc)
                    .sum();

            Long orderCount = orders.stream()
                    .filter(o -> o.getOrderDate().getYear() == monthDate.getYear() &&
                            o.getOrderDate().getMonthValue() == monthDate.getMonthValue())
                    .count();

            result.add(MonthlyRevenueResponse.builder()
                    .month(monthLabel)
                    .revenue(revenue)
                    .orderCount(orderCount)
                    .build());
        }
        return result;
    }

    private List<CategorySalesResponse> calculateSalesByCategory() {
        List<Category> categories = categoryRepository.findAll();
        List<CategorySalesResponse> result = new ArrayList<>();

        for (Category category : categories) {
            Double sales = 0.0;
            for (Product product : category.getProducts()) {
                sales += calculateRevenue(product);
            }

            if (sales > 0) {
                result.add(CategorySalesResponse.builder()
                        .categoryName(category.getName())
                        .sales(sales)
                        .productCount((long) category.getProducts().size())
                        .build());
            }
        }

        return result.stream()
                .sorted((a, b) -> b.getSales().compareTo(a.getSales()))
                .limit(6)
                .collect(Collectors.toList());
    }

    private Long calculateTotalSold(Product product) {
        if (product.getOrderItems() == null || product.getOrderItems().isEmpty()) return 0L;
        return product.getOrderItems().stream()
                .mapToLong(OrderItem::getQuantity)
                .sum();
    }

    private Double calculateRevenue(Product product) {
        if (product.getOrderItems() == null || product.getOrderItems().isEmpty()) return 0.0;
        return product.getOrderItems().stream()
                .mapToDouble(oi -> oi.getUnitPrice() * oi.getQuantity())
                .sum();
    }

    private OrderResponse convertToOrderResponse(Order order) {
        String customerName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(customerName)
                .status(order.getStatus())
                .totalTtc(order.getTotalTtc())
                .orderDate(order.getOrderDate())
                .build();
    }

    public SellerDashboardResponse getSellerDashboard() {
        log.info("Génération du dashboard SELLER");

        User currentUser = authService.getCurrentUser();
        SellerProfile sellerProfile = sellerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Profil vendeur non trouvé"));

        List<Product> sellerProducts = productRepository.findBySellerProfile(sellerProfile);
        List<Order> sellerOrders = orderRepository.findBySeller(sellerProfile);

        Double totalRevenue = sellerOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .mapToDouble(Order::getTotalTtc)
                .sum();

        Long totalOrders = (long) sellerOrders.size();
        Long pendingOrders = sellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.PAID)
                .count();

        List<ProductResponse> lowStockProducts = sellerProducts.stream()
                .filter(p -> p.getStock() < 10)
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());

        List<OrderResponse> recentOrders = sellerOrders.stream()
                .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                .limit(10)
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());

        return SellerDashboardResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .lowStockProducts(lowStockProducts)
                .recentOrders(recentOrders)
                .build();
    }

    private ProductResponse convertToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }
}