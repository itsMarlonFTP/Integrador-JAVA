package com.axity.curso.retailservice.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class InventoryDtos {
    private InventoryDtos() {
    }

    @Schema(name = "StockResponse", description = "Existencias vigentes de un producto.")
    public record StockResponse(Long productId, String sku, Integer stock, Integer minimumStock) {
    }

    @Schema(name = "ReplenishmentRequest", description = "Cantidad positiva que se suma a las existencias.")
    public record ReplenishmentRequest(@NotNull @Positive Integer quantity) {
    }

    @Schema(name = "InventoryAdjustmentRequest", description = "Variación firmada de stock; el resultado no puede ser negativo.")
    public record AdjustmentRequest(@NotNull Integer quantityDelta) {
    }

    @Schema(name = "WarehouseMovementResponse", description = "Movimiento de inventario procesado y auditado.")
    public record MovementResponse(Long id, Long productId, String type, Integer quantityDelta,
            Integer stockAfter, Long orderId, Instant occurredAt) {
    }
}
