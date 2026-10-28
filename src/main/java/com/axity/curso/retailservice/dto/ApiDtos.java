package com.axity.curso.retailservice.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public final class ApiDtos {
    private ApiDtos() {
    }

    @Schema(name = "ApiErrorResponse", description = "Formato consistente de los errores HTTP.")
    public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
    }

    @Schema(name = "AsyncEventResponse", description = "Resultado de una tarea secundaria; no implica envío de una notificación externa.")
    public record AsyncEventResponse(Long id, String eventType, String details, boolean successful,
            Instant occurredAt) {
    }
}
