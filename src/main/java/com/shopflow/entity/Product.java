package com.shopflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // CORRECTION ICI - Relation avec SellerProfile au lieu de User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    private SellerProfile sellerProfile;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private Double price;

    @Formula("CASE WHEN promo_price IS NOT NULL AND promo_price > 0 THEN promo_price ELSE price END")
    private Double effectivePriceForQuery;
    
    private Double promoPrice;
    
    private Integer stock;
    
    private boolean active = true;
    
    private String image;
    
    private LocalDateTime createdAt;
    
    @ManyToMany
    @JoinTable(
        name = "product_categories",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories = new ArrayList<>();
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();
    
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
private List<OrderItem> orderItems = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Calcule la moyenne des notes (avis approuvés)
    public Double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .filter(Review::isApproved)
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
    // Compte le nombre d'avis approuvés
    public int getReviewCount() {
        if (reviews == null) return 0;
        return (int) reviews.stream().filter(Review::isApproved).count();
    }
    // Retourne prix promo si dispo, sinon prix norm
    public Double getEffectivePrice() {
        return promoPrice != null && promoPrice > 0 ? promoPrice : price;
    }




}