package ru.arman.creditservice.domain.exceptions;

import java.util.UUID;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(UUID applicationId) {
        super("Credit application not found: " + applicationId);
    }
}
