package ru.arman.creditservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ApplicationCancelledEvent(
        UUID eventId,
        UUID applicationId,
        String correlationId,
        Instant occurredAt,
        String reason
) {
}
