package com.shopflow.service;

import com.shopflow.dto.request.CouponRequest;
import com.shopflow.dto.response.CouponResponse;
import com.shopflow.entity.Coupon;
import com.shopflow.exception.CouponException;
import com.shopflow.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new CouponException("Code promo déjà existant");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .type(request.getType())
                .valeur(request.getValue())  // ✅ valeur
                .dateExpiration(request.getExpirationDate())  // ✅ dateExpiration
                .usagesMax(request.getMaxUsages())  // ✅ usagesMax
                .usagesActuels(0)  // ✅ usagesActuels
                .active(true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon créé: {}", saved.getCode());
        return toResponse(saved);
    }

    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponException("Coupon non trouvé"));

        // Check if code already exists for another coupon
        if (!coupon.getCode().equals(request.getCode().toUpperCase()) &&
                couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new CouponException("Code promo déjà existant");
        }

        coupon.setCode(request.getCode().toUpperCase());
        coupon.setType(request.getType());
        coupon.setValeur(request.getValue());  // ✅ setValeur
        coupon.setDateExpiration(request.getExpirationDate());  // ✅ setDateExpiration
        coupon.setUsagesMax(request.getMaxUsages());  // ✅ setUsagesMax

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon mis à jour: {}", saved.getCode());
        return toResponse(saved);
    }

    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponException("Coupon non trouvé"));
        coupon.setActive(false);
        couponRepository.save(coupon);
        log.info("Coupon désactivé: {}", coupon.getCode());
    }

    public CouponResponse validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new CouponException("Code promo invalide"));

        if (!coupon.isValid()) {
            throw new CouponException("Code promo expiré ou déjà utilisé");
        }

        // Increment usage count
        coupon.setUsagesActuels(coupon.getUsagesActuels() + 1);  // ✅ usagesActuels
        couponRepository.save(coupon);
        log.info("Coupon validé: {} (utilisation {}/{})",
                coupon.getCode(), coupon.getUsagesActuels(), coupon.getUsagesMax());

        return toResponse(coupon);
    }

    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponException("Coupon non trouvé"));
        return toResponse(coupon);
    }

    public List<CouponResponse> getActiveCoupons() {
        return couponRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .type(coupon.getType())
                .value(coupon.getValeur())  // ✅ valeur
                .expirationDate(coupon.getDateExpiration())  // ✅ dateExpiration
                .maxUsages(coupon.getUsagesMax())  // ✅ usagesMax
                .currentUsages(coupon.getUsagesActuels())  // ✅ usagesActuels
                .active(coupon.getActive())
                .valid(coupon.isValid())
                .build();
    }
}