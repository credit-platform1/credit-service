package ru.arman.creditservice.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationCreatedEvent(
        UUID eventId,
        UUID applicationId,
        String correlationId,
        Instant occurredAt,
        CustomerPayload customer,
        LoanPayload loan
) {
    public record CustomerPayload(
            String firstName,
            String lastName,
            String middleName,
            String birthDate,
            String inn,
            String phone,
            String email
    ) {
    }

    public record LoanPayload(
            BigDecimal requestedAmount,
            Integer termMonths,
            String purpose,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses,
            String employmentType
    ) {
    }
}
