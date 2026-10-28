package com.axity.curso.retailservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.axity.curso.retailservice.model.AsyncEventLog;
import com.axity.curso.retailservice.repository.AsyncEventLogRepository;
import com.axity.curso.retailservice.service.RetailAsyncEventListener;
import com.axity.curso.retailservice.service.event.RetailAsyncEvent;
import com.axity.curso.retailservice.service.port.PurchaseNotificationPort;
import com.axity.curso.retailservice.service.port.ReplenishmentAlertPort;
import com.axity.curso.retailservice.service.port.WarehouseAuditPort;

class RetailAsyncEventListenerTests {
    @Test
    void secondaryFailureIsLoggedAndDoesNotEscapeTheListener() {
        PurchaseNotificationPort notifications = mock(PurchaseNotificationPort.class);
        WarehouseAuditPort audit = mock(WarehouseAuditPort.class);
        ReplenishmentAlertPort alerts = mock(ReplenishmentAlertPort.class);
        AsyncEventLogRepository eventLogs = mock(AsyncEventLogRepository.class);
        doThrow(new IllegalStateException("simulated adapter failure"))
                .when(notifications).notifyOrderConfirmed(42L);
        RetailAsyncEventListener listener = new RetailAsyncEventListener(notifications, audit, alerts, eventLogs);

        assertDoesNotThrow(() -> listener.handle(
                new RetailAsyncEvent("ORDER_CONFIRMED", 42L, null, null, null, null, null)));

        ArgumentCaptor<AsyncEventLog> savedLog = forClass(AsyncEventLog.class);
        verify(eventLogs).save(savedLog.capture());
        assertFalse(savedLog.getValue().isSuccessful());
    }
}
