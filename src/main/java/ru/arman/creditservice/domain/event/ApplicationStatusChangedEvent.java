package ru.arman.creditservice.domain.event;

import ru.arman.creditservice.domain.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusChangedEvent(
        UUID eventId,
        UUID applicationId,
        String correlationId,
        Instant occurredAt,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String changedBy,
        String reason
) {
}
