package ru.arman.creditservice.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateCreditApplicationRequest(
        @NotNull @Valid CustomerRequest customer,
        @NotNull @Valid LoanRequest loan
) {
}
