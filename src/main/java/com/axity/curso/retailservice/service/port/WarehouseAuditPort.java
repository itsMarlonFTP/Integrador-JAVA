package com.axity.curso.retailservice.service.port;

import com.axity.curso.retailservice.service.event.RetailAsyncEvent;

public interface WarehouseAuditPort {
    void recordMovement(RetailAsyncEvent event);
}
