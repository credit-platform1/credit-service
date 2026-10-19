package ru.arman.creditservice.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreditMetricsTest {

    private SimpleMeterRegistry registry;
    private CreditMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new CreditMetrics(registry);
    }

    @Test
    @DisplayName("Should increment applications created")
    void shouldIncrementApplicationsCreated() {
        metrics.incrementApplicationsCreated();

        var count = registry.find("credit.applications.created").counter().count();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should increment applications cancelled")
    void shouldIncrementApplicationsCancelled() {
        metrics.incrementApplicationsCancelled();

        var count = registry.find("credit.applications.cancelled").counter().count();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should increment status changed")
    void shouldIncrementStatusChangedWithTags() {
        metrics.incrementStatusChanged("NEW", "APPROVED");

        var count = registry.find("credit.applications.status.changed").counter().count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should increment outbox published with eventType tag")
    void shouldIncrementOutboxPublishedWithEventType() {
        metrics.incrementOutboxPublished("APPLICATION_CREATED");

        var count = registry.find("credit.outbox.events.published").counter().count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should increment outbox retry with eventType tag")
    void shouldIncrementOutboxRetryWithEventType() {
        metrics.incrementOutboxRetry("APPLICATION_CREATED");

        var count = registry.find("credit.outbox.events.retried").counter().count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should increment outbox failed with eventType tag")
    void shouldIncrementOutboxFailedWithEventType() {
        metrics.incrementOutboxFailed("APPLICATION_CREATED");

        var count = registry.find("credit.outbox.events.failed").counter().count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should have application=credit-service tag on all metrics")
    void shouldHaveApplicationTagOnAllMetrics() {
        metrics.incrementApplicationsCreated();
        metrics.incrementApplicationsCancelled();
        metrics.incrementStatusChanged("NEW", "APPROVED");
        metrics.incrementOutboxPublished("APPLICATION_CREATED");
        metrics.incrementOutboxRetry("APPLICATION_CREATED");
        metrics.incrementOutboxFailed("APPLICATION_CREATED");

        var allCounters = java.util.List.of(
                registry.find("credit.applications.created").counter(),
                registry.find("credit.applications.cancelled").counter(),
                registry.find("credit.applications.status.changed").counter(),
                registry.find("credit.outbox.events.published").counter(),
                registry.find("credit.outbox.events.retried").counter(),
                registry.find("credit.outbox.events.failed").counter()
        );

        for (var counter : allCounters) {
            assertThat(counter).isNotNull();
            assertThat(counter.getId().getTag("application")).isEqualTo("credit-service");
        }
    }

    @Test
    @DisplayName("Should not have high-cardinality tags")
    void shouldNotHaveHighCardinalityTags() {
        metrics.incrementApplicationsCreated();
        metrics.incrementStatusChanged("NEW", "APPROVED");

        var counters = java.util.List.of(
                registry.find("credit.applications.created").counter(),
                registry.find("credit.applications.status.changed").counter()
        );

        for (var counter : counters) {
            var tagKeys = java.util.stream.StreamSupport
                    .stream(counter.getId().getTags().spliterator(), false)
                    .map(tag -> tag.getKey())
                    .toList();

            assertThat(tagKeys).doesNotContain("applicationId", "correlationId", "customerInn", "phone", "email");
        }
    }
}
