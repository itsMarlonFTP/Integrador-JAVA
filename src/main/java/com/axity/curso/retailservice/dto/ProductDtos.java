package com.axity.curso.retailservice.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class ProductDtos {
    private ProductDtos() {
    }

    @Schema(name = "ProductCreateRequest", description = "Datos para crear un producto con existencias iniciales.")
    public record CreateRequest(
            @NotBlank @Size(max = 50) String sku,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            @Size(max = 50) String category,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal price,
            @NotNull @PositiveOrZero Integer stock,
            @NotNull @PositiveOrZero Integer minimumStock) {
    }

    @Schema(name = "ProductUpdateRequest", description = "Datos editables del catálogo; el stock se modifica desde Inventario.")
    public record UpdateRequest(
            @NotBlank @Size(max = 50) String sku,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            @Size(max = 50) String category,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal price,
            @NotNull @PositiveOrZero Integer minimumStock) {
    }

    @Schema(name = "ProductResponse", description = "Representación pública de un producto.")
    public record Response(Long id, String sku, String name, String description, String category,
            BigDecimal price, Integer stock, Integer minimumStock, boolean active) {
    }
}
