package com.axity.curso.retailservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.axity.curso.retailservice.dto.InventoryDtos.AdjustmentRequest;
import com.axity.curso.retailservice.dto.InventoryDtos.MovementResponse;
import com.axity.curso.retailservice.dto.InventoryDtos.ReplenishmentRequest;
import com.axity.curso.retailservice.dto.InventoryDtos.StockResponse;
import com.axity.curso.retailservice.model.WarehouseMovement;
import com.axity.curso.retailservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Product", description = "Entidad Product: existencias, reabastecimiento, ajustes y movimientos.")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Consultar existencias")
    public StockResponse get(@PathVariable Long productId) {
        return service.getStock(productId);
    }

    @PostMapping("/{productId}/replenishments")
    @Operation(summary = "Reabastecer inventario", description = "Suma una cantidad positiva al stock.")
    public StockResponse replenish(@PathVariable Long productId, @Valid @RequestBody ReplenishmentRequest request) {
        return service.replenish(productId, request);
    }

    @PostMapping("/{productId}/adjustments")
    @Operation(summary = "Ajustar inventario", description = "Aplica un delta positivo o negativo; el resultado nunca puede ser negativo.")
    public StockResponse adjust(@PathVariable Long productId, @Valid @RequestBody AdjustmentRequest request) {
        return service.adjust(productId, request);
    }

    @GetMapping("/{productId}/movements")
    @Operation(summary = "Consultar movimientos auditados", description = "La auditoría se registra asincrónicamente después del commit.")
    public Page<MovementResponse> movements(@PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.movements(productId, pageable).map(InventoryController::toResponse);
    }

    private static MovementResponse toResponse(WarehouseMovement movement) {
        return new MovementResponse(movement.getId(), movement.getProductId(), movement.getType(),
                movement.getQuantityDelta(), movement.getStockAfter(), movement.getOrderId(),
                movement.getOccurredAt());
    }
}
