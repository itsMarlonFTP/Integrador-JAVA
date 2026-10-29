package com.axity.curso.retailservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.axity.curso.retailservice.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class OrderDtos {
    private OrderDtos() {
    }

    @Schema(name = "OrderItemRequest", description = "Producto y cantidad solicitada. El cliente no envía precios.")
    public record ItemRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {
    }

    @Schema(name = "OrderCreateRequest", description = "Cliente y uno o más ítems para confirmar una compra.")
    public record CreateRequest(
            @NotBlank @Size(max = 100) String customerId,
            @NotEmpty @Size(max = 100) List<@Valid ItemRequest> items) {
    }

    @Schema(name = "OrderItemResponse", description = "Detalle con precio histórico calculado por el servidor.")
    public record ItemResponse(Long productId, String sku, String productName, Integer quantity,
            BigDecimal unitPrice, BigDecimal subtotal) {
    }

    @Schema(name = "OrderResponse", description = "Pedido confirmado con sus detalles, estado y total.")
    public record Response(Long id, String customerId, OrderStatus status, BigDecimal total,
            Instant createdAt, List<ItemResponse> items) {
    }

    @Schema(name = "OrderStatusRequest", description = "Estado solicitado; la transición válida es EN_PROCESO a ENVIADO.")
    public record StatusRequest(@NotNull OrderStatus status) {
    }
}
