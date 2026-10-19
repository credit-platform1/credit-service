package ru.arman.creditservice.service;

import ru.arman.creditservice.domain.model.ApplicationStatus;

import java.util.Map;
import java.util.Set;

public final class StatusTransitionPolicy {
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = Map.of(
            ApplicationStatus.NEW, Set.of(
                    ApplicationStatus.IN_PROGRESS,
                    ApplicationStatus.CANCELLED,
                    ApplicationStatus.FAILED
            ),

            ApplicationStatus.IN_PROGRESS, Set.of(
                    ApplicationStatus.WAITING_DOCUMENTS,
                    ApplicationStatus.MANUAL_REVIEW,
                    ApplicationStatus.APPROVED,
                    ApplicationStatus.DECLINED,
                    ApplicationStatus.CANCELLED,
                    ApplicationStatus.FAILED
            ),

            ApplicationStatus.WAITING_DOCUMENTS, Set.of(
                    ApplicationStatus.IN_PROGRESS,
                    ApplicationStatus.CANCELLED,
                    ApplicationStatus.FAILED
            ),

            ApplicationStatus.MANUAL_REVIEW, Set.of(
                    ApplicationStatus.APPROVED,
                    ApplicationStatus.DECLINED,
                    ApplicationStatus.CANCELLED,
                    ApplicationStatus.FAILED
            )
    );

    private StatusTransitionPolicy() {
        throw new AssertionError("Instantiating utility class.");
    }

    public static boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == to) {
            return false;
        }

        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
