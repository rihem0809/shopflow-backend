package com.shopflow.specification;

import com.shopflow.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filterByActive(boolean active) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("active"), active);
    }

    public static Specification<Product> filterByCategory(Long categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.join("categories").get("id"), categoryId);
        };
    }

    public static Specification<Product> filterByPriceRange(Double minPrice, Double maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> filterBySeller(Long sellerId) {
        return (root, query, criteriaBuilder) -> {
            if (sellerId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("sellerProfile").get("user").get("id"), sellerId);
        };
    }

    // ✅ CORRECTION: Gérer promo=true ET promo=false
    public static Specification<Product> filterByPromo(Boolean promo) {
        return (root, query, criteriaBuilder) -> {
            if (promo == null) {
                return criteriaBuilder.conjunction();
            }

            if (promo) {
                // PROMO = TRUE: produits avec promoPrice > 0
                return criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("promoPrice")),
                        criteriaBuilder.greaterThan(root.get("promoPrice"), 0)
                );
            } else {
                // PROMO = FALSE: produits SANS promo (promoPrice est null ou <= 0)
                return criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("promoPrice")),
                        criteriaBuilder.lessThanOrEqualTo(root.get("promoPrice"), 0)
                );
            }
        };
    }

    public static Specification<Product> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
            );
        };
    }
}