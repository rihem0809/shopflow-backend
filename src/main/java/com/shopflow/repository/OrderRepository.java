package com.shopflow.repository;

import com.shopflow.entity.Order;
import com.shopflow.entity.SellerProfile;
import com.shopflow.entity.User;
import com.shopflow.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ========== COMMANDES CLIENT ==========
    Page<Order> findByCustomerOrderByOrderDateDesc(User customer, Pageable pageable);

    List<Order> findByCustomer(User customer);

    // ========== TOUTES COMMANDES ==========
    Page<Order> findAllByOrderByOrderDateDesc(Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    // ========== COMMANDES VENDEUR ==========

    // ✅ Méthode 1: Avec @Query (recommandée)
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.sellerProfile = :seller ORDER BY o.orderDate DESC")
    List<Order> findBySeller(@Param("seller") SellerProfile seller);

    // ✅ Méthode 2: Version avec pagination
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.sellerProfile = :seller ORDER BY o.orderDate DESC")
    Page<Order> findBySeller(@Param("seller") SellerProfile seller, Pageable pageable);

    // ✅ Méthode 3: findBySellerProfile (nom conventionnel)
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.sellerProfile = :sellerProfile ORDER BY o.orderDate DESC")
    Page<Order> findBySellerProfile(@Param("sellerProfile") SellerProfile sellerProfile, Pageable pageable);

    // ✅ Méthode 4: Alternative avec nom de méthode Spring Data (plus longue)
    Page<Order> findByItems_Product_SellerProfile(SellerProfile sellerProfile, Pageable pageable);

    // ✅ Méthode 5: Pour compter les commandes d'un vendeur
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product.sellerProfile = :seller")
    long countBySeller(@Param("seller") SellerProfile seller);
}