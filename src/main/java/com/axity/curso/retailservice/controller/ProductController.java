package com.axity.curso.retailservice.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.axity.curso.retailservice.dto.ProductDtos.CreateRequest;
import com.axity.curso.retailservice.dto.ProductDtos.Response;
import com.axity.curso.retailservice.dto.ProductDtos.UpdateRequest;
import com.axity.curso.retailservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "Entidad Product: catálogo y disponibilidad del producto.")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear producto", description = "Recibe ProductCreateRequest con SKU único y existencias iniciales.")
    public Response create(@Valid @RequestBody CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Buscar productos", description = "Lista productos activos; permite filtrar por categoría y precios.")
    public Page<Response> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minimumPrice,
            @RequestParam(required = false) BigDecimal maximumPrice,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.search(category, minimumPrice, maximumPrice, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar producto")
    public Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto", description = "Recibe ProductUpdateRequest; actualiza datos de catálogo. "
            + "El stock se modifica solo por endpoints de inventario.")
    public Response update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar producto", description = "Soft delete; el producto se conserva para el historial.")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
