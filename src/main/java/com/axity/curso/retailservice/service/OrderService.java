package com.axity.curso.retailservice.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.axity.curso.retailservice.dto.OrderDtos.CreateRequest;
import com.axity.curso.retailservice.dto.OrderDtos.ItemResponse;
import com.axity.curso.retailservice.dto.OrderDtos.Response;
import com.axity.curso.retailservice.exception.ConflictoOperacionException;
import com.axity.curso.retailservice.exception.PrecioInvalidoException;
import com.axity.curso.retailservice.exception.RecursoNoEncontradoException;
import com.axity.curso.retailservice.exception.StockInsuficienteException;
import com.axity.curso.retailservice.model.OrderItem;
import com.axity.curso.retailservice.model.OrderStatus;
import com.axity.curso.retailservice.model.Product;
import com.axity.curso.retailservice.model.Order;
import com.axity.curso.retailservice.repository.ProductRepository;
import com.axity.curso.retailservice.repository.OrderRepository;
import com.axity.curso.retailservice.service.event.RetailAsyncEvent;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, ProductRepository products, ApplicationEventPublisher events) {
        this.orders = orders;
        this.products = products;
        this.events = events;
    }

    @Transactional
    public Response create(CreateRequest request) {
        Map<Long, Integer> quantitiesByProduct = new TreeMap<>();
        try {
            request.items().forEach(item -> quantitiesByProduct.merge(item.productId(), item.quantity(), Math::addExact));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("La cantidad total solicitada excede el límite permitido");
        }

        Map<Long, Product> lockedProducts = new TreeMap<>();
        for (Long productId : quantitiesByProduct.keySet()) {
            Product product = products.findActiveForUpdate(productId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + productId + " no encontrado"));
            int requestedQuantity = quantitiesByProduct.get(productId);
            if (product.getStock() < requestedQuantity) {
                throw new StockInsuficienteException("Stock insuficiente para el producto " + productId
                        + ": disponible " + product.getStock() + ", solicitado " + requestedQuantity);
            }
            lockedProducts.put(productId, product);
        }

        Order order = new Order(request.customerId().trim());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, Integer> stockBefore = new TreeMap<>();
        for (Map.Entry<Long, Integer> entry : quantitiesByProduct.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            int previousStock = product.getStock();
            stockBefore.put(product.getId(), previousStock);
            product.setStock(previousStock - entry.getValue());
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(entry.getValue())).setScale(2);
            total = total.add(subtotal);
            order.addItem(new OrderItem(product, entry.getValue(), unitPrice));
        }
        if (total.precision() > 12) {
            throw new PrecioInvalidoException("El total del pedido excede el importe máximo permitido");
        }
        order.setTotal(total);
        order = orders.saveAndFlush(order);
        for (Map.Entry<Long, Integer> entry : quantitiesByProduct.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            publishMovement(order, product, -entry.getValue(), "VENTA");
            if (stockBefore.get(product.getId()) > product.getMinimumStock()
                    && product.getStock() <= product.getMinimumStock()) {
                events.publishEvent(new RetailAsyncEvent("LOW_STOCK", null, product.getId(), null,
                        product.getStock(), product.getMinimumStock(), null));
            }
        }
        events.publishEvent(new RetailAsyncEvent("ORDER_CONFIRMED", order.getId(), null, null, null, null, null));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return orders.findDetailedById(id).map(OrderService::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<Response> list(Pageable pageable) {
        return orders.findAllOrders(pageable).map(OrderService::toResponse);
    }

    @Transactional
    public Response changeStatus(Long id, OrderStatus requestedStatus) {
        Order order = orders.findForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido " + id + " no encontrado"));
        if (order.getStatus() != OrderStatus.EN_PROCESO || requestedStatus != OrderStatus.ENVIADO) {
            throw new ConflictoOperacionException("Transición de estado inválida: " + order.getStatus()
                    + " -> " + requestedStatus);
        }
        order.setStatus(OrderStatus.ENVIADO);
        return orders.findDetailedById(id).map(OrderService::toResponse).orElseThrow();
    }

    @Transactional
    public Response cancel(Long id) {
        Order order = orders.findForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido " + id + " no encontrado"));
        if (order.getStatus() == OrderStatus.CANCELADO) {
            return orders.findDetailedById(id).map(OrderService::toResponse).orElseThrow();
        }
        if (order.getStatus() != OrderStatus.EN_PROCESO) {
            throw new ConflictoOperacionException("Solo se pueden cancelar pedidos EN_PROCESO");
        }

        List<OrderItem> items = new ArrayList<>(order.getItems());
        Map<Long, Product> lockedProducts = new TreeMap<>();
        items.stream().map(item -> item.getProduct().getId()).distinct().sorted().forEach(productId -> {
            Product product = products.findForUpdate(productId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + productId + " no encontrado"));
            lockedProducts.put(productId, product);
        });
        for (OrderItem item : items) {
            Product product = lockedProducts.get(item.getProduct().getId());
            try {
                product.setStock(Math.addExact(product.getStock(), item.getQuantity()));
            } catch (ArithmeticException exception) {
                throw new ConflictoOperacionException("La reversión excede la capacidad de inventario");
            }
            publishMovement(order, product, item.getQuantity(), "DEVOLUCION_CANCELACION");
        }
        order.setStatus(OrderStatus.CANCELADO);
        return orders.findDetailedById(id).map(OrderService::toResponse).orElseThrow();
    }

    private void publishMovement(Order order, Product product, int delta, String movementType) {
        events.publishEvent(new RetailAsyncEvent("WAREHOUSE_AUDIT", order.getId(), product.getId(), delta,
                product.getStock(), product.getMinimumStock(), movementType));
    }

    private static Response toResponse(Order order) {
        List<ItemResponse> items = order.getItems().stream()
                .map(item -> new ItemResponse(item.getProduct().getId(), item.getProduct().getSku(),
                        item.getProduct().getName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal()))
                .toList();
        return new Response(order.getId(), order.getCustomerId(), order.getStatus(), order.getTotal(),
                order.getCreatedAt(), items);
    }
}
