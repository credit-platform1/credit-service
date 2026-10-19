package ru.arman.creditservice.domain.dto;

import ru.arman.creditservice.domain.model.ApplicationStatus;

import java.time.Instant;

public record CreditApplicationResponse(
        String applicationId,
        ApplicationStatus status,
        CustomerResponse customer,
        LoanResponse loan,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
