package com.axity.curso.retailservice.service.event;

public record RetailAsyncEvent(String type, Long orderId, Long productId, Integer quantityDelta,
        Integer stockAfter, Integer minimumStock, String movementType) {
}
