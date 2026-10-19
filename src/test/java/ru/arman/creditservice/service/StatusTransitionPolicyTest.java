package ru.arman.creditservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.arman.creditservice.domain.model.ApplicationStatus;

import static org.assertj.core.api.Assertions.assertThat;

class StatusTransitionPolicyTest {

    @Nested
    @DisplayName("NEW status transitions")
    class NewTransitions {

        @Test
        @DisplayName("NEW -> IN_PROGRESS should be allowed")
        void newToInProgress_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.NEW, ApplicationStatus.IN_PROGRESS))
                    .isTrue();
        }

        @Test
        @DisplayName("NEW -> CANCELLED should be allowed")
        void newToCancelled_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.NEW, ApplicationStatus.CANCELLED))
                    .isTrue();
        }

        @Test
        @DisplayName("NEW -> FAILED should be allowed")
        void newToFailed_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.NEW, ApplicationStatus.FAILED))
                    .isTrue();
        }

        @Test
        @DisplayName("NEW -> APPROVED should NOT be allowed")
        void newToApproved_shouldNotBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.NEW, ApplicationStatus.APPROVED))
                    .isFalse();
        }

        @Test
        @DisplayName("NEW -> NEW (same) should NOT be allowed")
        void newToNew_shouldNotBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.NEW, ApplicationStatus.NEW))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("IN_PROGRESS status transitions")
    class InProgressTransitions {

        @Test
        @DisplayName("IN_PROGRESS -> WAITING_DOCUMENTS should be allowed")
        void inProgressToWaitingDocuments_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.IN_PROGRESS, ApplicationStatus.WAITING_DOCUMENTS))
                    .isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS -> MANUAL_REVIEW should be allowed")
        void inProgressToManualReview_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.IN_PROGRESS, ApplicationStatus.MANUAL_REVIEW))
                    .isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS -> APPROVED should be allowed")
        void inProgressToApproved_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.IN_PROGRESS, ApplicationStatus.APPROVED))
                    .isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS -> DECLINED should be allowed")
        void inProgressToDeclined_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.IN_PROGRESS, ApplicationStatus.DECLINED))
                    .isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS -> CANCELLED should be allowed")
        void inProgressToCancelled_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.IN_PROGRESS, ApplicationStatus.CANCELLED))
                    .isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS -> NEW should NOT be allowed")
        void inProgressToNew_shouldNotBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.IN_PROGRESS, ApplicationStatus.NEW))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Terminal status transitions")
    class TerminalTransitions {

        @Test
        @DisplayName("APPROVED -> any transition should NOT be allowed")
        void approvedToAny_shouldNotBeAllowed() {
            for (ApplicationStatus status : ApplicationStatus.values()) {
                if (status != ApplicationStatus.APPROVED) {
                    assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.APPROVED, status))
                            .as("APPROVED -> %s should not be allowed", status)
                            .isFalse();
                }
            }
        }

        @Test
        @DisplayName("DECLINED -> any transition should NOT be allowed")
        void declinedToAny_shouldNotBeAllowed() {
            for (ApplicationStatus status : ApplicationStatus.values()) {
                if (status != ApplicationStatus.DECLINED) {
                    assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.DECLINED, status))
                            .as("DECLINED -> %s should not be allowed", status)
                            .isFalse();
                }
            }
        }

        @Test
        @DisplayName("CANCELLED -> any transition should NOT be allowed")
        void cancelledToAny_shouldNotBeAllowed() {
            for (ApplicationStatus status : ApplicationStatus.values()) {
                if (status != ApplicationStatus.CANCELLED) {
                    assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.CANCELLED, status))
                            .as("CANCELLED -> %s should not be allowed", status)
                            .isFalse();
                }
            }
        }

        @Test
        @DisplayName("FAILED -> any transition should NOT be allowed")
        void failedToAny_shouldNotBeAllowed() {
            for (ApplicationStatus status : ApplicationStatus.values()) {
                if (status != ApplicationStatus.FAILED) {
                    assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.FAILED, status))
                            .as("FAILED -> %s should not be allowed", status)
                            .isFalse();
                }
            }
        }
    }

    @Nested
    @DisplayName("WAITING_DOCUMENTS status transitions")
    class WaitingDocumentsTransitions {

        @Test
        @DisplayName("WAITING_DOCUMENTS -> IN_PROGRESS should be allowed")
        void waitingToInProgress_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.WAITING_DOCUMENTS, ApplicationStatus.IN_PROGRESS))
                    .isTrue();
        }

        @Test
        @DisplayName("WAITING_DOCUMENTS -> CANCELLED should be allowed")
        void waitingToCancelled_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.WAITING_DOCUMENTS, ApplicationStatus.CANCELLED))
                    .isTrue();
        }

        @Test
        @DisplayName("WAITING_DOCUMENTS -> APPROVED should NOT be allowed")
        void waitingToApproved_shouldNotBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.WAITING_DOCUMENTS, ApplicationStatus.APPROVED))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("MANUAL_REVIEW status transitions")
    class ManualReviewTransitions {

        @Test
        @DisplayName("MANUAL_REVIEW -> APPROVED should be allowed")
        void manualToApproved_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.APPROVED))
                    .isTrue();
        }

        @Test
        @DisplayName("MANUAL_REVIEW -> DECLINED should be allowed")
        void manualToDeclined_shouldBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.DECLINED))
                    .isTrue();
        }

        @Test
        @DisplayName("MANUAL_REVIEW -> IN_PROGRESS should NOT be allowed")
        void manualToInProgress_shouldNotBeAllowed() {
            assertThat(StatusTransitionPolicy.canTransition(ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.IN_PROGRESS))
                    .isFalse();
        }
    }
}
