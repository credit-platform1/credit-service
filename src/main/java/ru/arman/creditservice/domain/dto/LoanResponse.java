package ru.arman.creditservice.domain.dto;

import ru.arman.creditservice.domain.model.EmploymentType;
import ru.arman.creditservice.domain.model.LoanPurpose;

import java.math.BigDecimal;

public record LoanResponse(
        BigDecimal requestedAmount,
        Integer termMonths,
        LoanPurpose purpose,
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpenses,
        EmploymentType employmentType
) {
}
