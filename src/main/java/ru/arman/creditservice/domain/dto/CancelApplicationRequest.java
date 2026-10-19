package ru.arman.creditservice.domain.dto;

import jakarta.validation.constraints.Size;

public record CancelApplicationRequest(
        @Size(max = 500) String reason
) {
}
