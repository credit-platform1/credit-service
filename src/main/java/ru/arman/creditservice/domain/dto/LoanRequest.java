package ru.arman.creditservice.domain.dto;

import jakarta.validation.constraints.*;
import ru.arman.creditservice.domain.model.EmploymentType;
import ru.arman.creditservice.domain.model.LoanPurpose;

import java.math.BigDecimal;

public record LoanRequest(
        @NotNull @DecimalMin("10000.00") @DecimalMax("5000000.00")
        BigDecimal requestedAmount,

        @NotNull @Min(3) @Max(120)
        Integer termMonths,

        @NotNull LoanPurpose purpose,

        @NotNull @DecimalMin("0.00")
        BigDecimal monthlyIncome,

        @NotNull @DecimalMin("0.00")
        BigDecimal monthlyExpenses,

        @NotNull EmploymentType employmentType
) {
}
