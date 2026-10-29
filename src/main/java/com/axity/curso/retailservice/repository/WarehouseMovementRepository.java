package com.axity.curso.retailservice.repository;

import com.axity.curso.retailservice.model.WarehouseMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseMovementRepository extends JpaRepository<WarehouseMovement, Long> {
    Page<WarehouseMovement> findByProductIdOrderByOccurredAtDesc(Long productId, Pageable pageable);
}
