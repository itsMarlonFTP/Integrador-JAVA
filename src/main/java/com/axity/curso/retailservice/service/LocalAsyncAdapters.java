package com.axity.curso.retailservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.axity.curso.retailservice.model.AsyncEventLog;
import com.axity.curso.retailservice.model.WarehouseMovement;
import com.axity.curso.retailservice.repository.AsyncEventLogRepository;
import com.axity.curso.retailservice.repository.WarehouseMovementRepository;
import com.axity.curso.retailservice.service.event.RetailAsyncEvent;
import com.axity.curso.retailservice.service.port.PurchaseNotificationPort;
import com.axity.curso.retailservice.service.port.ReplenishmentAlertPort;
import com.axity.curso.retailservice.service.port.WarehouseAuditPort;

@Component
public class LocalAsyncAdapters implements PurchaseNotificationPort, WarehouseAuditPort, ReplenishmentAlertPort {
    private static final Logger log = LoggerFactory.getLogger(LocalAsyncAdapters.class);
    private final AsyncEventLogRepository eventLogs;
    private final WarehouseMovementRepository movements;

    public LocalAsyncAdapters(AsyncEventLogRepository eventLogs, WarehouseMovementRepository movements) {
        this.eventLogs = eventLogs;
        this.movements = movements;
    }

    @Override
    public void notifyOrderConfirmed(Long orderId) {
        String details = "Confirmación de compra simulada para pedido " + orderId;
        log.info(details);
        eventLogs.save(new AsyncEventLog("NOTIFICACION_COMPRA", details, true));
    }

    @Override
    public void recordMovement(RetailAsyncEvent event) {
        movements.save(new WarehouseMovement(event.productId(), event.movementType(), event.quantityDelta(),
                event.stockAfter(), event.orderId()));
        eventLogs.save(new AsyncEventLog("AUDITORIA_INVENTARIO",
                "Movimiento " + event.movementType() + " de producto " + event.productId(), true));
    }

    @Override
    public void alertLowStock(Long productId, Integer stock, Integer minimumStock) {
        String details = "Alerta simulada: producto " + productId + " con stock " + stock
                + " (mínimo " + minimumStock + ")";
        log.warn(details);
        eventLogs.save(new AsyncEventLog("ALERTA_REABASTECIMIENTO", details, true));
    }
}
