package ru.arman.creditservice.infrastructure.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.arman.creditservice.domain.entity.OutboxEventEntity;
import ru.arman.creditservice.domain.event.ApplicationEventType;
import ru.arman.creditservice.infrastructure.metrics.CreditMetrics;
import ru.arman.creditservice.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxKafkaPublisherMetricsTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopics topics;

    private CreditMetrics metrics;
    private OutboxKafkaPublisher publisher;

    private io.micrometer.core.instrument.simple.SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        metrics = new CreditMetrics(registry);
        publisher = new OutboxKafkaPublisher(outboxRepository, kafkaTemplate, topics, metrics);
    }

    @Test
    @DisplayName("Should increment outboxPublished on successful publish")
    void shouldIncrementOutboxPublishedOnSuccess() throws Exception {
        var event = createEvent(ApplicationEventType.APPLICATION_CREATED);
        var result = createSendResult();
        doReturn(java.util.List.of(event)).when(outboxRepository).findTop100ByStatusOrderByCreatedAtAscNative();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(topics.applicationCreated()).thenReturn("test-topic");

        publisher.publishPendingEvents();

        assertThat(metrics.outboxPublished.count()).isOne();
    }

    @Test
    @DisplayName("Should increment outboxRetried on publish failure")
    void shouldIncrementOutboxRetriedOnFailure() throws Exception {
        var event = createEvent(ApplicationEventType.APPLICATION_CREATED);
        var future = new CompletableFuture<SendResult<String, String>>();
        future.completeExceptionally(new RuntimeException("Kafka unavailable"));
        doReturn(java.util.List.of(event)).when(outboxRepository).findTop100ByStatusOrderByCreatedAtAscNative();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(future);
        when(topics.applicationCreated()).thenReturn("test-topic");

        publisher.publishPendingEvents();

        assertThat(metrics.outboxRetried.count()).isOne();
    }

    @Test
    @DisplayName("Should increment outboxFailed when retry limit reached")
    void shouldIncrementOutboxFailedWhenRetryLimitReached() throws Exception {
        var event = createEvent(ApplicationEventType.APPLICATION_CREATED);
        event.setRetryCount(5); // At max retries
        var future = new CompletableFuture<SendResult<String, String>>();
        future.completeExceptionally(new RuntimeException("Kafka unavailable"));
        doReturn(java.util.List.of(event)).when(outboxRepository).findTop100ByStatusOrderByCreatedAtAscNative();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(future);
        when(topics.applicationCreated()).thenReturn("test-topic");

        publisher.publishPendingEvents();

        assertThat(metrics.outboxFailed.count()).isOne();
    }

    @Test
    @DisplayName("Should record outbox publish duration timer")
    void shouldRecordOutboxPublishDurationTimer() throws Exception {
        var event = createEvent(ApplicationEventType.APPLICATION_CREATED);
        var result = createSendResult();
        doReturn(java.util.List.of(event)).when(outboxRepository).findTop100ByStatusOrderByCreatedAtAscNative();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(result));
        when(topics.applicationCreated()).thenReturn("test-topic");

        publisher.publishPendingEvents();

        assertThat(metrics.outboxPublishDuration.count()).isOne();
    }

    private OutboxEventEntity createEvent(ApplicationEventType eventType) {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .aggregateId(UUID.randomUUID())
                .correlationId("test-correlation")
                .payload("{\"test\":\"payload\"}")
                .status(OutboxEventEntity.OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build();
    }

    private SendResult<String, String> createSendResult() {
        var metadata = mock(RecordMetadata.class);
        when(metadata.topic()).thenReturn("test-topic");
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(0L);
        return new SendResult<>(null, metadata);
    }
}
