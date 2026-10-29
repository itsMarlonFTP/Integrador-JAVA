package com.axity.curso.retailservice.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "async_event_logs")
public class AsyncEventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false, length = 1000)
    private String details;

    @Column(nullable = false)
    private boolean successful;

    @Column(nullable = false)
    private Instant occurredAt = Instant.now();

    protected AsyncEventLog() {
    }

    public AsyncEventLog(String eventType, String details, boolean successful) {
        this.eventType = eventType;
        this.details = details;
        this.successful = successful;
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public String getDetails() { return details; }
    public boolean isSuccessful() { return successful; }
    public Instant getOccurredAt() { return occurredAt; }
}
