package ru.arman.creditservice.domain.dto;

import java.time.LocalDate;

public record CustomerResponse(
        String firstName,
        String lastName,
        String middleName,
        LocalDate birthDate,
        String phone,
        String email
) {
}
