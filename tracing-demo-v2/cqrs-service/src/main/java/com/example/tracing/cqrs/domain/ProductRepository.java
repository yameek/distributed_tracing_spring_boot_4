package com.example.tracing.cqrs.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Product aggregate.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    List<Product> findByStatus(Product.ProductStatus status);
    
    List<Product> findByNameContainingIgnoreCase(String name);
}
