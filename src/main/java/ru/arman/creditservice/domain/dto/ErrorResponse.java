package ru.arman.creditservice.domain.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String correlationId,
        List<FieldError> errors
) {
    public record FieldError(
            String field,
            String message
    ) {
    }
}