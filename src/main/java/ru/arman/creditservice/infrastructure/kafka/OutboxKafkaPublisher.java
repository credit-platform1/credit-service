package ru.arman.creditservice.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.arman.creditservice.domain.entity.OutboxEventEntity;
import ru.arman.creditservice.domain.event.ApplicationEventType;
import ru.arman.creditservice.infrastructure.metrics.CreditMetrics;
import ru.arman.creditservice.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxKafkaPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopics topics;
    private final CreditMetrics metrics;

    @Value("${outbox.publisher.max-retries:5}")
    private int maxRetries;

    @Scheduled(
            fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}"
    )
    @Transactional
    public void publishPendingEvents() {
        var events = outboxRepository.findTop100ByStatusOrderByCreatedAtAscNative();

        for (var event : events) {
            var topic = resolveTopic(event);

            if (topic == null) {
                log.error("No topic configured for event type {}", event.getEventType());

                event.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
                event.setLastError("No topic configured for event type: " + event.getEventType());
                continue;
            }

            try {
                var sample = metrics.startOutboxPublishTimer();
                SendResult<String, String> result = kafkaTemplate
                        .send(
                                topic,
                                event.getAggregateId().toString(),
                                event.getPayload()
                        )
                        .get(5, TimeUnit.SECONDS);
                metrics.stopOutboxPublishTimer(sample);

                log.info(
                        "Outbox event published: eventId={}, topic={}, partition={}, offset={}",
                        event.getId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );

                event.setStatus(OutboxEventEntity.OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
                metrics.incrementOutboxPublished(event.getEventType().name());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage());
                metrics.incrementOutboxRetry(event.getEventType().name());

                if (event.getRetryCount() >= maxRetries) {
                    event.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
                    metrics.incrementOutboxFailed(event.getEventType().name());
                }

                log.error(
                        "Failed to publish outbox event: eventId={}, retry={}, error={}",
                        event.getId(),
                        event.getRetryCount(),
                        e.getMessage(),
                        e
                );
            }
        }
    }

    private String resolveTopic(OutboxEventEntity event) {
        return switch (event.getEventType()) {
            case APPLICATION_CREATED -> topics.applicationCreated();
            case APPLICATION_CANCELLED -> topics.applicationCancelled();
            case APPLICATION_STATUS_CHANGED -> topics.applicationStatusChanged();
        };
    }
}