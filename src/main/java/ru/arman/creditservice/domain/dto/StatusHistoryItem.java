package ru.arman.creditservice.domain.dto;

import ru.arman.creditservice.domain.model.ApplicationStatus;

import java.time.Instant;

public record StatusHistoryItem(
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String changedBy,
        String reason,
        Instant changedAt
) {
}
