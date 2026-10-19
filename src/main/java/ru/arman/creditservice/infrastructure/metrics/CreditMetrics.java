package ru.arman.creditservice.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Бизнес-метрики credit-service.
 * Не использует high-cardinality tags (applicationId, correlationId, customerInn и др. PII).
 */
@Component
public class CreditMetrics {

    private static final String APPLICATION_TAG = "application";
    private static final String APPLICATION_NAME = "credit-service";

    private final MeterRegistry registry;
    public final Counter applicationsCreated;
    public final Counter applicationsCancelled;
    public final Counter statusChanged;
    public final Counter outboxPublished;
    public final Counter outboxRetried;
    public final Counter outboxFailed;
    public final Timer outboxPublishDuration;

    public CreditMetrics(MeterRegistry registry) {
        this.registry = registry;
        var commonTags = List.of(Tag.of(APPLICATION_TAG, APPLICATION_NAME));

        this.applicationsCreated = Counter.builder("credit.applications.created")
                .description("Total credit applications created")
                .tag(APPLICATION_TAG, APPLICATION_NAME)
                .register(registry);

        this.applicationsCancelled = Counter.builder("credit.applications.cancelled")
                .description("Total credit applications cancelled")
                .tag(APPLICATION_TAG, APPLICATION_NAME)
                .register(registry);

        this.statusChanged = Counter.builder("credit.applications.status.changed")
                .description("Total credit application status changes")
                .tag(APPLICATION_TAG, APPLICATION_NAME)
                .register(registry);

        this.outboxPublished = Counter.builder("credit.outbox.events.published")
                .description("Total outbox events published to Kafka")
                .tag(APPLICATION_TAG, APPLICATION_NAME)
                .register(registry);

        this.outboxRetried = Counter.builder("credit.outbox.events.retried")
                .description("Total outbox event retries")
                .tag(APPLICATION_TAG, APPLICATION_NAME)
                .register(registry);

        this.outboxFailed = Counter.builder("credit.outbox.events.failed")
                .description("Total outbox events that failed after max retries")
                .tag(APPLICATION_TAG, APPLICATION_NAME)
                .register(registry);

        this.outboxPublishDuration = Timer.builder("credit.outbox.publish.duration")
                .description("Duration of outbox event publishing to Kafka")
                .tag(APPLICATION_TAG, APPLICATION_NAME)
                .register(registry);
    }

    public void incrementApplicationsCreated() {
        applicationsCreated.increment();
    }

    public void incrementApplicationsCancelled() {
        applicationsCancelled.increment();
    }

    public void incrementStatusChanged(String fromStatus, String toStatus) {
        statusChanged.increment();
    }

    public void incrementOutboxPublished(String eventType) {
        outboxPublished.increment();
    }

    public void incrementOutboxRetry(String eventType) {
        outboxRetried.increment();
    }

    public void incrementOutboxFailed(String eventType) {
        outboxFailed.increment();
    }

    public Timer.Sample startOutboxPublishTimer() {
        return Timer.start(registry);
    }

    public void stopOutboxPublishTimer(Timer.Sample sample) {
        sample.stop(outboxPublishDuration);
    }
}
