package ru.arman.creditservice.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.arman.creditservice.domain.event.ApplicationEventType;
import ru.arman.creditservice.repository.OutboxEventRepository;
import ru.arman.creditservice.test.TestFixtures;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.arman.creditservice.test.TestFixtures.CORRELATION_ID;

@Testcontainers
@DataJpaTest
class OutboxEventEntityDataJpaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OutboxEventRepository repository;

    @Test
    @DisplayName("Entity should auto-generate UUID on persist")
    void persist_autoGeneratesUUID() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("Entity should persist and retrieve by event type")
    void persistAndFindByEventType() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEventType()).isEqualTo(ApplicationEventType.APPLICATION_CREATED);
        assertThat(found.get().getStatus()).isEqualTo(OutboxEventEntity.OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("Entity should persist correlation ID")
    void persist_correlationId() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCorrelationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    @DisplayName("Entity should update status to PUBLISHED")
    void updateStatusToPublished() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);
        var id = saved.getId();

        saved.setStatus(OutboxEventEntity.OutboxStatus.PUBLISHED);
        saved.setPublishedAt(Instant.now());
        repository.save(saved);

        var updated = repository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxEventEntity.OutboxStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Entity should update status to FAILED and set retry count")
    void updateStatusToFailed() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);
        var id = saved.getId();

        saved.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
        saved.setRetryCount(3);
        saved.setLastError("Connection timeout");
        repository.save(saved);

        var updated = repository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxEventEntity.OutboxStatus.FAILED);
        assertThat(updated.getRetryCount()).isEqualTo(3);
        assertThat(updated.getLastError()).isEqualTo("Connection timeout");
    }

    @Test
    @DisplayName("Entity should find by aggregate ID")
    void findByAggregateId() {
        var aggregateId = UUID.randomUUID();
        var entity = createEntity(aggregateId);
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAggregateId()).isEqualTo(aggregateId);
    }



    @Test
    @DisplayName("Entity should persist payload as JSONB")
    void persist_payload() {
        var payload = "{\"eventId\":\"123\",\"applicationId\":\"456\"}";
        var entity = createEntity(UUID.randomUUID());
        entity.setPayload(payload);
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPayload()).isEqualTo(payload);
    }

    private OutboxEventEntity createEntity(UUID aggregateId) {
        return TestFixtures.createOutboxEventEntity(aggregateId);
    }
}
