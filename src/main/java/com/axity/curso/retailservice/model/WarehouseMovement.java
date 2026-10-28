package com.axity.curso.retailservice.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse_movements")
public class WarehouseMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false)
    private Integer quantityDelta;

    @Column(nullable = false)
    private Integer stockAfter;

    @Column
    private Long orderId;

    @Column(nullable = false)
    private Instant occurredAt = Instant.now();

    protected WarehouseMovement() {
    }

    public WarehouseMovement(Long productId, String type, Integer quantityDelta, Integer stockAfter, Long orderId) {
        this.productId = productId;
        this.type = type;
        this.quantityDelta = quantityDelta;
        this.stockAfter = stockAfter;
        this.orderId = orderId;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getType() { return type; }
    public Integer getQuantityDelta() { return quantityDelta; }
    public Integer getStockAfter() { return stockAfter; }
    public Long getOrderId() { return orderId; }
    public Instant getOccurredAt() { return occurredAt; }
}
