package com.shopflow.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shopflow.entity.Product;
import com.shopflow.entity.SellerProfile;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // ✅ Pour la liste complète (sans pagination)
    List<Product> findByActiveTrue();

    // Pour la pagination
    Page<Product> findByActiveTrue(Pageable pageable);

    // ✅ AJOUTER cette méthode
    Page<Product> findBySellerProfileAndActiveTrue(SellerProfile sellerProfile, Pageable pageable);

    // ✅ AJOUTER cette méthode
    List<Product> findBySellerProfile(SellerProfile sellerProfile);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.promoPrice IS NOT NULL AND p.promoPrice > 0")
    Page<Product> findPromoProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY SIZE(p.orderItems) DESC")
    List<Product> findTopSellingProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.sellerProfile = :seller AND p.stock < :threshold")
    List<Product> findLowStockProducts(@Param("seller") SellerProfile seller, @Param("threshold") int threshold);

    // ✅ AJOUTER cette méthode pour compter les produits par vendeur
    @Query("SELECT COUNT(p) FROM Product p WHERE p.sellerProfile = :seller")
    long countBySellerProfile(@Param("seller") SellerProfile seller);

    // ✅ AJOUTER cette méthode alternative
    long countBySellerProfileId(Long sellerProfileId);
}