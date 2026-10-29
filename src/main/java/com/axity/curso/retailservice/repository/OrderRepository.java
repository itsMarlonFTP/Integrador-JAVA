package com.axity.curso.retailservice.repository;

import java.util.Optional;

import com.axity.curso.retailservice.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("select distinct o from RetailOrder o where o.id = :id")
    Optional<Order> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from RetailOrder o where o.id = :id")
    Optional<Order> findForUpdate(@Param("id") Long id);

    @Query("select o from RetailOrder o")
    Page<Order> findAllOrders(Pageable pageable);
}
