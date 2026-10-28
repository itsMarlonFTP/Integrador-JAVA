package com.axity.curso.retailservice.service.port;

public interface PurchaseNotificationPort {
    void notifyOrderConfirmed(Long orderId);
}
