package ru.arman.creditservice.domain.dto;

import ru.arman.creditservice.domain.model.ApplicationStatus;

import java.time.Instant;
import java.util.Map;

public record CreateCreditApplicationResponse(
        String applicationId,
        ApplicationStatus status,
        Instant createdAt,
        Map<String, String> links
) {
}
