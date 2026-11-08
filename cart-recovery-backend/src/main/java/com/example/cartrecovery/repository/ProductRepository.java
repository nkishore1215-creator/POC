package com.example.cartrecovery.repository;

import com.example.cartrecovery.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {


    Optional<Product> findById(Long productId);

    Optional<Product> findByProductName(String productName);

    List<Product> findByStockQuantityGreaterThan(Integer quantity);

    List<Product> findByProductNameContainingIgnoreCase(String keyword);

    boolean existsByProductName(String productName);
}
