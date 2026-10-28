package com.axity.curso.retailservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.axity.curso.retailservice.dto.ApiDtos.AsyncEventResponse;
import com.axity.curso.retailservice.model.AsyncEventLog;
import com.axity.curso.retailservice.repository.AsyncEventLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/async-events")
@Tag(name = "Order", description = "Entidad Order y resultados asincrónicos relacionados con pedidos.")
public class AsyncEventController {
    private final AsyncEventLogRepository eventLogs;

    public AsyncEventController(AsyncEventLogRepository eventLogs) {
        this.eventLogs = eventLogs;
    }

    @GetMapping
    @Operation(summary = "Consultar resultados asincrónicos", description = "Exhibe resultados de tareas completadas; las notificaciones locales son simuladas.")
    public Page<AsyncEventResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return eventLogs.findAllByOrderByOccurredAtDesc(pageable).map(AsyncEventController::toResponse);
    }

    private static AsyncEventResponse toResponse(AsyncEventLog event) {
        return new AsyncEventResponse(event.getId(), event.getEventType(), event.getDetails(),
                event.isSuccessful(), event.getOccurredAt());
    }
}
