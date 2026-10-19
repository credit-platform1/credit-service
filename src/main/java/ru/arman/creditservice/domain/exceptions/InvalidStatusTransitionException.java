package ru.arman.creditservice.domain.exceptions;

import ru.arman.creditservice.domain.model.ApplicationStatus;

import java.util.UUID;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus
    ) {
        super("Invalid status transition: %s -> %s".formatted(fromStatus, toStatus));
    }
}
