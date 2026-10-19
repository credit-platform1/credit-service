package ru.arman.creditservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.arman.creditservice.domain.model.ApplicationStatus;

public record ChangeApplicationStatusRequest(
        @NotNull ApplicationStatus status,
        @Size(max = 500) String reason,
        @NotBlank @Size(max = 100) String changedBy
) {
}
