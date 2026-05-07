package com.shopflow.service;

import com.shopflow.dto.request.OrderRequest;
import com.shopflow.dto.response.OrderResponse;
import com.shopflow.dto.response.OrderItemResponse;
import com.shopflow.dto.response.AddressResponse;
import com.shopflow.entity.*;
import com.shopflow.entity.enums.OrderStatus;
import com.shopflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final AuthService authService;

    private static final double SHIPPING_FEE = 7.99;
    private static final double FREE_SHIPPING_THRESHOLD = 100.0;

    // ==================== CREATE ORDER ====================
    public OrderResponse createOrder(OrderRequest request) {
        User customer = authService.getCurrentUser();
        log.info("Création commande pour: {}", customer.getEmail());

        // Vérifier le panier
        Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new RuntimeException("Panier non trouvé"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Le panier est vide");
        }

        // Vérifier l'adresse de livraison
        Address shippingAddress = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Adresse de livraison non trouvée"));

        // Vérification du stock finale
        for (CartItem item : cart.getItems()) {
            int availableStock = getAvailableStock(item.getProduct(), item.getVariant());
            if (availableStock < item.getQuantity()) {
                throw new RuntimeException("Stock insuffisant pour: " + item.getProduct().getName() +
                        ". Disponible: " + availableStock + ", demandé: " + item.getQuantity());
            }
        }

        // Calcul des montants
        double subtotal = cart.getItems().stream()
                .mapToDouble(i -> getItemPrice(i) * i.getQuantity())
                .sum();

        // Appliquer coupon si présent
        double discount = 0;
        Coupon coupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
            coupon = couponRepository.findByCodeAndActiveTrue(request.getCouponCode().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Code promo invalide"));

            if (!coupon.isValid()) {
                throw new RuntimeException("Code promo expiré ou déjà utilisé");
            }

            discount = coupon.calculateDiscount(subtotal);
            coupon.incrementUsage();
            couponRepository.save(coupon);
        }

        double afterDiscount = subtotal - discount;
        double shippingFee = (afterDiscount >= FREE_SHIPPING_THRESHOLD) ? 0 : SHIPPING_FEE;
        double total = afterDiscount + shippingFee;

        // Génération d'un numéro de commande unique
        String orderNumber = generateOrderNumber();

        // Création de la commande
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .shippingAddress(shippingAddress)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalTtc(total)
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .isNew(true)
                .items(new ArrayList<>())
                .build();

        // Créer les items et réduire le stock
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .variant(cartItem.getVariant())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(getItemPrice(cartItem))
                    .build();
            order.getItems().add(orderItem);

            // Réduire le stock
            reduceStock(cartItem.getProduct(), cartItem.getVariant(), cartItem.getQuantity());
        }

        Order saved = orderRepository.save(order);

        // Vider le panier
        cart.getItems().clear();
        cart.setAppliedCoupon(null);
        cartRepository.save(cart);

        log.info("Commande créée: {} - Numéro: {}", saved.getId(), saved.getOrderNumber());

        // Simulation de paiement automatique
        simulatePayment(saved);

        return toResponse(saved);
    }

    // ==================== SIMULATION PAIEMENT ====================
    private void simulatePayment(Order order) {
        order.setStatus(OrderStatus.PAID);
        order.setNew(true);
        orderRepository.save(order);
        log.info("Paiement simulé pour commande: {}", order.getOrderNumber());

        order.setStatus(OrderStatus.PROCESSING);
        order.setNew(true);
        orderRepository.save(order);
        log.info("Commande en traitement: {}", order.getOrderNumber());
    }

    // ==================== UPDATE STATUS (SELLER & ADMIN) ====================
    public OrderResponse updateStatus(Long id, String statusStr) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        User currentUser = authService.getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isSeller = currentUser.getRole().name().equals("SELLER");

        if (!isAdmin && !isSeller) {
            throw new RuntimeException("Vous n'avez pas les droits pour modifier le statut");
        }

        // ✅ Vérifier que le vendeur ne modifie que SES commandes
        if (isSeller && !isAdmin) {
            boolean containsSellerProduct = order.getItems().stream()
                    .anyMatch(item -> item.getProduct().getSellerProfile() != null &&
                            item.getProduct().getSellerProfile().getId()
                                    .equals(currentUser.getSellerProfile().getId()));

            if (!containsSellerProduct) {
                throw new RuntimeException("Vous ne pouvez modifier que les commandes contenant vos produits");
            }
        }

        try {
            OrderStatus newStatus = OrderStatus.valueOf(statusStr.toUpperCase());

            if (!order.canTransitionTo(newStatus)) {
                throw new RuntimeException("Changement de statut invalide: " +
                        order.getStatus() + " -> " + newStatus);
            }

            order.setStatus(newStatus);
            order.setNew(true);

            Order saved = orderRepository.save(order);
            log.info("Commande {}: {} -> {}", saved.getOrderNumber(), order.getStatus(), newStatus);
            return toResponse(saved);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + statusStr);
        }
    }

    // ==================== CANCEL ORDER ====================
    public OrderResponse cancelOrder(Long id) {
        User customer = authService.getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Vous ne pouvez annuler que vos propres commandes");
        }

        if (!order.isCancellable()) {
            throw new RuntimeException("Cette commande ne peut plus être annulée. Statut actuel: " +
                    order.getStatus().getDescription());
        }

        // Annuler la commande
        order.setStatus(OrderStatus.CANCELLED);
        order.setNew(true);

        // Restaurer le stock
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null) {
                item.getVariant().setExtraStock(item.getVariant().getExtraStock() + item.getQuantity());
            } else {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        log.info("Remboursement simulé pour la commande {}: Montant de {} TND",
                order.getOrderNumber(), order.getTotalTtc());

        Order saved = orderRepository.save(order);
        log.info("Commande annulée: {}", saved.getOrderNumber());
        return toResponse(saved);
    }

    // ==================== GET ORDER BY ID (avec vérification droits) ====================
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        User currentUser = authService.getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isSeller = currentUser.getRole().name().equals("SELLER");
        boolean isOwner = order.getCustomer().getId().equals(currentUser.getId());

        // ✅ Vérification pour SELLER : doit contenir ses produits
        if (isSeller && !isAdmin && !isOwner) {
            boolean containsSellerProduct = order.getItems().stream()
                    .anyMatch(item -> item.getProduct().getSellerProfile() != null &&
                            item.getProduct().getSellerProfile().getId()
                                    .equals(currentUser.getSellerProfile().getId()));

            if (!containsSellerProduct) {
                throw new RuntimeException("Vous n'avez pas accès à cette commande");
            }
        } else if (!isAdmin && !isSeller && !isOwner) {
            throw new RuntimeException("Vous n'avez pas accès à cette commande");
        }

        return toResponse(order);
    }

    // ==================== GET MY ORDERS (CUSTOMER) ====================
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User customer = authService.getCurrentUser();
        return orderRepository.findByCustomerOrderByOrderDateDesc(customer, pageable)
                .map(this::toResponse);
    }

    // ==================== GET MY SELLER ORDERS (SELLER) ====================
    public Page<OrderResponse> getMySellerOrders(Pageable pageable, String status) {
        User currentUser = authService.getCurrentUser();

        // Vérifier que l'utilisateur est un vendeur avec un profil
        if (!currentUser.getRole().name().equals("SELLER")) {
            throw new RuntimeException("Accès réservé aux vendeurs");
        }

        if (currentUser.getSellerProfile() == null) {
            throw new RuntimeException("Profil vendeur non trouvé");
        }

        SellerProfile sellerProfile = currentUser.getSellerProfile();

        // Récupérer les commandes
        Page<Order> orders = orderRepository.findBySeller(sellerProfile, pageable);

        // Filtrer par statut si demandé
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus filterStatus = OrderStatus.valueOf(status.toUpperCase());
                List<Order> filteredOrders = orders.getContent().stream()
                        .filter(order -> order.getStatus() == filterStatus)
                        .collect(Collectors.toList());

                orders = new PageImpl<>(filteredOrders, pageable, filteredOrders.size());
            } catch (IllegalArgumentException e) {
                log.warn("Statut invalide pour filtrage: {}", status);
            }
        }

        return orders.map(this::toResponse);
    }

    // ==================== GET ALL ORDERS (ADMIN) ====================
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByOrderDateDesc(pageable)
                .map(this::toResponse);
    }

    // ==================== STATS POUR VENDEUR ====================
    public Map<String, Long> getMySellerOrdersCount() {
        User currentUser = authService.getCurrentUser();

        if (!currentUser.getRole().name().equals("SELLER") || currentUser.getSellerProfile() == null) {
            throw new RuntimeException("Profil vendeur non trouvé");
        }

        SellerProfile sellerProfile = currentUser.getSellerProfile();
        List<Order> orders = orderRepository.findBySeller(sellerProfile);

        Map<String, Long> stats = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = orders.stream().filter(o -> o.getStatus() == status).count();
            stats.put(status.name(), count);
        }

        return stats;
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private double getItemPrice(CartItem item) {
        double basePrice = item.getProduct().getEffectivePrice();
        if (item.getVariant() != null && item.getVariant().getPriceDelta() != null) {
            return basePrice + item.getVariant().getPriceDelta();
        }
        return basePrice;
    }

    private int getAvailableStock(Product product, ProductVariant variant) {
        if (variant != null) {
            return variant.getExtraStock() != null ? variant.getExtraStock() : 0;
        }
        return product.getStock() != null ? product.getStock() : 0;
    }

    private void reduceStock(Product product, ProductVariant variant, int quantity) {
        if (variant != null) {
            variant.setExtraStock(variant.getExtraStock() - quantity);
        } else {
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
        }
    }

    private String generateOrderNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + year + "-" + random;
    }

    // ==================== CONVERSION ENTITÉ → DTO ====================
    private OrderResponse toResponse(Order order) {
        String customerName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();

        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productImage(item.getProduct().getImage() != null ?
                                item.getProduct().getImage() : "https://via.placeholder.com/80x80")
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        AddressResponse addressResponse = null;
        if (order.getShippingAddress() != null) {
            addressResponse = AddressResponse.builder()
                    .id(order.getShippingAddress().getId())
                    .street(order.getShippingAddress().getStreet())
                    .city(order.getShippingAddress().getCity())
                    .postalCode(order.getShippingAddress().getPostalCode())
                    .country(order.getShippingAddress().getCountry())
                    .principal(order.getShippingAddress().isPrincipal())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .statusDescription(order.getStatus().getDescription())
                .customerName(customerName)
                .shippingAddress(addressResponse)
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .totalTtc(order.getTotalTtc())
                .items(items)
                .orderDate(order.getOrderDate())
                .isNew(order.isNew())
                .cancellable(order.isCancellable())
                .build();
    }
}