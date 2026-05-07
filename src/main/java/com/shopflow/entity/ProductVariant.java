package com.shopflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String attribute;

    @Column(name = "variant_value")  // ✅ Renommer pour éviter le mot réservé
    private String value;

    @Column(name = "extra_stock")
    private Integer extraStock;

    @Column(name = "price_delta")
    private Double priceDelta;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}