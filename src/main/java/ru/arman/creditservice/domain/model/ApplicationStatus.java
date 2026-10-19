package ru.arman.creditservice.domain.model;

public enum ApplicationStatus {
    NEW,
    IN_PROGRESS,
    WAITING_DOCUMENTS,
    MANUAL_REVIEW,
    APPROVED,
    DECLINED,
    CANCELLED,
    FAILED
}
