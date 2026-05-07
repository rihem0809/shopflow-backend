package com.shopflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopflow.entity.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeAndActiveTrue(String code);

    Optional<Coupon> findByCode(String code);

    // ✅ Ajouter cette méthode
    boolean existsByCode(String code);

    List<Coupon> findByActiveTrue();
}