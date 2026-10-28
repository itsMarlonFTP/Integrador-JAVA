package com.axity.curso.retailservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.axity.curso.retailservice.dto.InventoryDtos.AdjustmentRequest;
import com.axity.curso.retailservice.dto.InventoryDtos.ReplenishmentRequest;
import com.axity.curso.retailservice.dto.InventoryDtos.StockResponse;
import com.axity.curso.retailservice.exception.RecursoNoEncontradoException;
import com.axity.curso.retailservice.exception.StockInsuficienteException;
import com.axity.curso.retailservice.model.Product;
import com.axity.curso.retailservice.repository.ProductRepository;
import com.axity.curso.retailservice.repository.WarehouseMovementRepository;

@Service
public class InventoryService {
    private final ProductRepository products;
    private final WarehouseMovementRepository movements;
    private final ProductService productService;
    public InventoryService(ProductRepository products, WarehouseMovementRepository movements,
            ProductService productService) {
        this.products = products;
        this.movements = movements;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(Long productId) {
        Product product = productService.findActive(productId);
        return toResponse(product);
    }

    @Transactional
    public StockResponse replenish(Long productId, ReplenishmentRequest request) {
        Product product = lockActiveProduct(productId);
        int oldStock = product.getStock();
        try {
            product.setStock(Math.addExact(oldStock, request.quantity()));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("La cantidad reabastecida excede el límite permitido");
        }
        productService.publishMovement(product, "REABASTECIMIENTO", request.quantity(), null);
        productService.publishIfThresholdCrossed(product, oldStock);
        return toResponse(product);
    }

    @Transactional
    public StockResponse adjust(Long productId, AdjustmentRequest request) {
        Product product = lockActiveProduct(productId);
        if (request.quantityDelta() == 0) {
            throw new IllegalArgumentException("El ajuste de inventario no puede ser cero");
        }
        long updated = (long) product.getStock() + request.quantityDelta();
        if (updated < 0) {
            throw new StockInsuficienteException("El ajuste dejaría el inventario en un valor negativo");
        }
        if (updated > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El ajuste excede la capacidad de inventario");
        }
        int oldStock = product.getStock();
        product.setStock((int) updated);
        productService.publishMovement(product, "AJUSTE", request.quantityDelta(), null);
        productService.publishIfThresholdCrossed(product, oldStock);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<com.axity.curso.retailservice.model.WarehouseMovement> movements(Long productId, Pageable pageable) {
        if (!products.existsById(productId)) {
            throw new RecursoNoEncontradoException("Producto " + productId + " no encontrado");
        }
        return movements.findByProductIdOrderByOccurredAtDesc(productId, pageable);
    }

    private Product lockActiveProduct(Long id) {
        return products.findActiveForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
    }

    private StockResponse toResponse(Product product) {
        return new StockResponse(product.getId(), product.getSku(), product.getStock(), product.getMinimumStock());
    }
}
