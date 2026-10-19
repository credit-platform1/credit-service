package ru.arman.creditservice.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.arman.creditservice.repository.IdempotencyKeyRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
class IdempotencyKeyEntityDataJpaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private IdempotencyKeyRepository repository;

    @Test
    @DisplayName("Entity should persist and retrieve by idempotency key")
    void persistAndFindByIdempotencyKey() {
        var key = "test-idempotency-key-123";
        var applicationId = UUID.randomUUID();
        var entity = IdempotencyKeyEntity.builder()
                .idempotencyKey(key)
                .applicationId(applicationId)
                .requestHash("abc123")
                .createdAt(Instant.now())
                .build();

        var saved = repository.save(entity);

        var found = repository.findById(key);

        assertThat(found).isPresent();
        assertThat(found.get().getApplicationId()).isEqualTo(applicationId);
        assertThat(found.get().getRequestHash()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("Entity should not exist for unknown key")
    void findByIdempotencyKey_notFound() {
        var found = repository.findById("non-existent-key");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Entity should update applicationId")
    void update_applicationId() {
        var key = "test-idempotency-key-456";
        var entity = IdempotencyKeyEntity.builder()
                .idempotencyKey(key)
                .applicationId(UUID.randomUUID())
                .requestHash("abc123")
                .createdAt(Instant.now())
                .build();

        var saved = repository.save(entity);
        var initialAppId = saved.getApplicationId();

        var newAppId = UUID.randomUUID();
        saved.setApplicationId(newAppId);
        repository.save(saved);

        var updated = repository.findById(key).orElseThrow();
        assertThat(updated.getApplicationId()).isEqualTo(newAppId);
        assertThat(updated.getApplicationId()).isNotEqualTo(initialAppId);
    }
}
