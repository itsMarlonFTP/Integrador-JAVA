package com.axity.curso.retailservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.axity.curso.retailservice.model.AsyncEventLog;
import com.axity.curso.retailservice.repository.AsyncEventLogRepository;
import com.axity.curso.retailservice.service.event.RetailAsyncEvent;
import com.axity.curso.retailservice.service.port.PurchaseNotificationPort;
import com.axity.curso.retailservice.service.port.ReplenishmentAlertPort;
import com.axity.curso.retailservice.service.port.WarehouseAuditPort;

@Component
public class RetailAsyncEventListener {
    private static final Logger log = LoggerFactory.getLogger(RetailAsyncEventListener.class);
    private final PurchaseNotificationPort notifications;
    private final WarehouseAuditPort audit;
    private final ReplenishmentAlertPort alerts;
    private final AsyncEventLogRepository eventLogs;

    public RetailAsyncEventListener(PurchaseNotificationPort notifications, WarehouseAuditPort audit,
            ReplenishmentAlertPort alerts, AsyncEventLogRepository eventLogs) {
        this.notifications = notifications;
        this.audit = audit;
        this.alerts = alerts;
        this.eventLogs = eventLogs;
    }

    @Async("retailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RetailAsyncEvent event) {
        try {
            switch (event.type()) {
                case "ORDER_CONFIRMED" -> notifications.notifyOrderConfirmed(event.orderId());
                case "WAREHOUSE_AUDIT" -> audit.recordMovement(event);
                case "LOW_STOCK" -> alerts.alertLowStock(event.productId(), event.stockAfter(), event.minimumStock());
                default -> throw new IllegalArgumentException("Tipo de evento asincrónico desconocido: " + event.type());
            }
        } catch (RuntimeException exception) {
            log.error("Falló la tarea secundaria {}: {}", event.type(), exception.getMessage(), exception);
            try {
                eventLogs.save(new AsyncEventLog(event.type(), "Falló el procesamiento asincrónico; revisar logs",
                        false));
            } catch (RuntimeException loggingException) {
                log.error("No fue posible persistir el fallo asincrónico {}", event.type(), loggingException);
            }
        }
    }
}
