package ru.arman.creditservice.domain.exceptions;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key is already used with a different request: " + idempotencyKey);
    }
}
