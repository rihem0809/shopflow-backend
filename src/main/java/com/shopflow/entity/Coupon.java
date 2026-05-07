package com.shopflow.entity;

import com.shopflow.entity.enums.CouponType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;

    @Column(nullable = false)
    private Double valeur;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    @Column(name = "usages_max")
    private Integer usagesMax;

    @Column(name = "usages_actuels")
    private Integer usagesActuels;

    private Boolean active = true;

    // ✅ Méthode pour vérifier la validité du coupon
    public boolean isValid() {
        if (!active) return false;
        if (dateExpiration != null && dateExpiration.isBefore(LocalDateTime.now())) return false;
        if (usagesMax != null && usagesActuels != null && usagesActuels >= usagesMax) return false;
        return true;
    }

    // ✅ Méthode pour calculer la remise
    public double calculateDiscount(double subtotal) {
        if (type == CouponType.PERCENT) {
            return subtotal * (valeur / 100);
        } else {
            return Math.min(valeur, subtotal);
        }
    }

    // ✅ Méthode pour incrémenter le nombre d'utilisations
    public void incrementUsage() {
        if (usagesActuels == null) {
            usagesActuels = 0;
        }
        usagesActuels++;
    }

    // ✅ Getters avec noms compatibles
    public Double getValue() {
        return valeur;
    }

    public void setValue(Double value) {
        this.valeur = value;
    }

    public LocalDateTime getExpirationDate() {
        return dateExpiration;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.dateExpiration = expirationDate;
    }

    public Integer getMaxUsages() {
        return usagesMax;
    }

    public void setMaxUsages(Integer maxUsages) {
        this.usagesMax = maxUsages;
    }

    public Integer getCurrentUsages() {
        return usagesActuels;
    }

    public void setCurrentUsages(Integer currentUsages) {
        this.usagesActuels = currentUsages;
    }
}