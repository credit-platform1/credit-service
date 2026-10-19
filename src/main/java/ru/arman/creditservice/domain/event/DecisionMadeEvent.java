package ru.arman.creditservice.domain.event;

import ru.arman.creditservice.domain.model.ApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DecisionMadeEvent (
    UUID eventId,
    UUID applicationId,
    String correlationId,
    Instant occurredAt,
    ApplicationStatus decision,
    BigDecimal score,
    BigDecimal probabilityOfDefault,
    List<String> reasonCodes,
    String modelVersion,
    String policyVersion
) {
}
