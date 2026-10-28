package com.axity.curso.retailservice.service.port;

public interface ReplenishmentAlertPort {
    void alertLowStock(Long productId, Integer stock, Integer minimumStock);
}
