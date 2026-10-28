package com.axity.curso.retailservice.repository;

import java.math.BigDecimal;
import java.util.Optional;

import com.axity.curso.retailservice.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndActiveTrue(Long id);
    boolean existsBySkuIgnoreCase(String sku);

    @Query("""
            select p from Product p
            where p.active = true
              and (:category is null or lower(p.category) = lower(:category))
              and (:minimumPrice is null or p.price >= :minimumPrice)
              and (:maximumPrice is null or p.price <= :maximumPrice)
            """)
    Page<Product> searchActive(@Param("category") String category,
            @Param("minimumPrice") BigDecimal minimumPrice,
            @Param("maximumPrice") BigDecimal maximumPrice, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.active = true")
    Optional<Product> findActiveForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);
}
