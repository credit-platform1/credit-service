package ru.arman.creditservice.infrastructure.kafka;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arman.creditservice.domain.dto.ChangeApplicationStatusRequest;
import ru.arman.creditservice.domain.event.DecisionMadeEvent;
import ru.arman.creditservice.domain.exceptions.ApplicationNotFoundException;
import ru.arman.creditservice.domain.model.ApplicationStatus;
import ru.arman.creditservice.service.CreditApplicationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DecisionMadeConsumerTest {

    @Mock
    private CreditApplicationService applicationService;

    private DecisionMadeConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DecisionMadeConsumer(applicationService, ObservationRegistry.create());
    }

    @Test
    @DisplayName("Should transition to APPROVED when decision is APPROVED")
    void shouldTransitionToApproved() {
        var applicationId = UUID.randomUUID();
        var event = createEvent(applicationId, ApplicationStatus.APPROVED);

        consumer.handle(event);

        var captor = ArgumentCaptor.forClass(ChangeApplicationStatusRequest.class);
        verify(applicationService).changeStatus(eq(applicationId), captor.capture(), any());
        assertThat(captor.getValue().status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(captor.getValue().reason()).isEqualTo("test_reason");
        assertThat(captor.getValue().changedBy()).isEqualTo("SCORING_SERVICE");
    }

    @Test
    @DisplayName("Should transition to DECLINED when decision is DECLINED")
    void shouldTransitionToDeclined() {
        var applicationId = UUID.randomUUID();
        var event = createEvent(applicationId, ApplicationStatus.DECLINED);

        consumer.handle(event);

        var captor = ArgumentCaptor.forClass(ChangeApplicationStatusRequest.class);
        verify(applicationService).changeStatus(eq(applicationId), captor.capture(), any());
        assertThat(captor.getValue().status()).isEqualTo(ApplicationStatus.DECLINED);
    }

    @Test
    @DisplayName("Should transition to MANUAL_REVIEW when decision is MANUAL_REVIEW")
    void shouldTransitionToManualReview() {
        var applicationId = UUID.randomUUID();
        var event = createEvent(applicationId, ApplicationStatus.MANUAL_REVIEW);

        consumer.handle(event);

        var captor = ArgumentCaptor.forClass(ChangeApplicationStatusRequest.class);
        verify(applicationService).changeStatus(eq(applicationId), captor.capture(), any());
        assertThat(captor.getValue().status()).isEqualTo(ApplicationStatus.MANUAL_REVIEW);
    }

    @Test
    @DisplayName("Should throw exception for unsupported decision")
    void shouldThrowExceptionForUnsupportedDecision() {
        var applicationId = UUID.randomUUID();
        var event = new DecisionMadeEvent(
                UUID.randomUUID(),
                applicationId,
                "test-correlation",
                Instant.now(),
                ApplicationStatus.NEW, // Unsupported
                new BigDecimal("0.5"),
                new BigDecimal("0.1"),
                List.of("reason"),
                "1.0",
                "1.0"
        );

        assertThatThrownBy(() -> consumer.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported decision status");
    }

    @Test
    @DisplayName("Should propagate reason codes to change status request")
    void shouldPropagateReasonCodes() {
        var applicationId = UUID.randomUUID();
        var reasonCodes = List.of("low_score", "high_risk");
        var event = createEventWithReasons(applicationId, ApplicationStatus.APPROVED, reasonCodes);

        consumer.handle(event);

        var captor = ArgumentCaptor.forClass(ChangeApplicationStatusRequest.class);
        verify(applicationService).changeStatus(any(), captor.capture(), any());
        assertThat(captor.getValue().reason()).isEqualTo("low_score,high_risk");
    }

    @Test
    @DisplayName("Should propagate correlationId from event")
    void shouldPropagateCorrelationId() {
        var applicationId = UUID.randomUUID();
        var correlationId = "test-correlation-id";
        var event = createEvent(applicationId, ApplicationStatus.APPROVED, correlationId);

        consumer.handle(event);

        verify(applicationService).changeStatus(eq(applicationId), any(), eq(correlationId));
    }

    private DecisionMadeEvent createEvent(UUID applicationId, ApplicationStatus decision) {
        return createEvent(applicationId, decision, "test-correlation");
    }

    private DecisionMadeEvent createEvent(UUID applicationId, ApplicationStatus decision, String correlationId) {
        return new DecisionMadeEvent(
                UUID.randomUUID(),
                applicationId,
                correlationId,
                Instant.now(),
                decision,
                new BigDecimal("0.75"),
                new BigDecimal("0.15"),
                List.of("test_reason"),
                "1.0",
                "1.0"
        );
    }

    private DecisionMadeEvent createEventWithReasons(UUID applicationId, ApplicationStatus decision, List<String> reasonCodes) {
        return new DecisionMadeEvent(
                UUID.randomUUID(),
                applicationId,
                "test-correlation",
                Instant.now(),
                decision,
                new BigDecimal("0.75"),
                new BigDecimal("0.15"),
                reasonCodes,
                "1.0",
                "1.0"
        );
    }
}
