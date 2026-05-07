package com.shopflow.repository;

import com.shopflow.entity.Product;
import com.shopflow.entity.Review;
import com.shopflow.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductAndApprovedTrue(Product product, Pageable pageable);

    Page<Review> findByApprovedFalse(Pageable pageable);

    boolean existsByCustomerAndProduct(User customer, Product product);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product AND r.approved = true")
    Double getAverageRatingByProduct(@Param("product") Product product);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product = :product AND r.approved = true")
    Long countApprovedByProduct(@Param("product") Product product);
}