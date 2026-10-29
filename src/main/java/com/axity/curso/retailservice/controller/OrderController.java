package com.axity.curso.retailservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.axity.curso.retailservice.dto.OrderDtos.CreateRequest;
import com.axity.curso.retailservice.dto.OrderDtos.Response;
import com.axity.curso.retailservice.dto.OrderDtos.StatusRequest;
import com.axity.curso.retailservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order", description = "Entidad Order: compra, estado, detalle, transición y cancelación.")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(tags = {"Order", "OrderItem"}, summary = "Crear pedido",
            description = "Recibe OrderCreateRequest con customerId e items (productId y quantity). "
            + "Confirma sincrónicamente stock y totales; precios y total se calculan en el servidor.")
    public Response create(@Valid @RequestBody CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Listar pedidos")
    public Page<Response> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(tags = {"Order", "OrderItem"}, summary = "Consultar pedido",
            description = "Devuelve Order y sus OrderItem asociados.")
    public Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado", description = "Recibe OrderStatusRequest. La única transición operativa permitida es EN_PROCESO -> ENVIADO.")
    public Response changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return service.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar pedido", description = "Solo cancela pedidos EN_PROCESO. Repetir la solicitud es idempotente.")
    public Response cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
