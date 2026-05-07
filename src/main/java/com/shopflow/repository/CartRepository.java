package com.shopflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopflow.entity.Cart;
import com.shopflow.entity.User;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    Optional<Cart> findByCustomer(User customer);
    
    // Nouvelle méthode
    void deleteByCustomer(User customer);
}