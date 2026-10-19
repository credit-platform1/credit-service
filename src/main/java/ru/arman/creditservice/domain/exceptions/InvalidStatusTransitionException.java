package ru.arman.creditservice.domain.exceptions;

import ru.arman.creditservice.domain.model.ApplicationStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus
    ) {
        super("Invalid status transition: %s -> %s".formatted(fromStatus, toStatus));
    }
}
