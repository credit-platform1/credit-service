package ru.arman.creditservice.domain.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CustomerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String middleName,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Pattern(regexp = "\\d{10}|\\d{12}") String inn,
        @NotBlank @Pattern(regexp = "^\\+7\\d{10}$") String phone,
        @NotBlank @Email @Size(max = 255) String email
) {
}
