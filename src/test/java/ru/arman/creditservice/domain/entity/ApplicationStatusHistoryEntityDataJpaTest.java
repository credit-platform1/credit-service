package ru.arman.creditservice.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.arman.creditservice.domain.model.ApplicationStatus;
import ru.arman.creditservice.repository.ApplicationStatusHistoryRepository;
import ru.arman.creditservice.repository.CreditApplicationRepository;
import ru.arman.creditservice.test.TestFixtures;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.arman.creditservice.test.TestFixtures.CHANGED_BY;
import static ru.arman.creditservice.test.TestFixtures.VALID_STATUS;

@Testcontainers
@DataJpaTest
class ApplicationStatusHistoryEntityDataJpaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ApplicationStatusHistoryRepository repository;

    @Autowired
    private CreditApplicationRepository creditAppRepository;

    @Test
    @DisplayName("Entity should auto-generate UUID on persist")
    void persist_autoGeneratesUUID() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("Entity should persist fromStatus as null")
    void persist_fromStatusNull() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFromStatus()).isNull();
    }

    @Test
    @DisplayName("Entity should persist toStatus")
    void persist_toStatus() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getToStatus()).isEqualTo(VALID_STATUS);
    }

    @Test
    @DisplayName("Entity should persist correlation ID")
    void persist_correlationId() {
        var correlationId = "test-correlation-456";
        var entity = createEntity(UUID.randomUUID());
        entity.setCorrelationId(correlationId);
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Entity should persist reason")
    void persist_reason() {
        var entity = createEntity(UUID.randomUUID());
        entity.setReason("Test reason");
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getReason()).isEqualTo("Test reason");
    }

    @Test
    @DisplayName("Entity should persist changedBy")
    void persist_changedBy() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getChangedBy()).isEqualTo(CHANGED_BY);
    }

    @Test
    @DisplayName("Entity should find by application ID")
    void findByApplicationId() {
        var applicationId = UUID.randomUUID();
        creditAppRepository.save(TestFixtures.createCreditApplicationEntity(applicationId));
        var entity = createEntity(applicationId);
        repository.save(entity);

        var found = repository.findByApplicationIdOrderByChangedAtAsc(applicationId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getApplicationId()).isEqualTo(applicationId);
    }

    @Test
    @DisplayName("Entity should return empty list for unknown application ID")
    void findByApplicationId_empty() {
        var found = repository.findByApplicationIdOrderByChangedAtAsc(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Entity should return ordered history for multiple entries")
    void findByApplicationId_ordered() throws InterruptedException {
        var applicationId = UUID.randomUUID();
        creditAppRepository.save(TestFixtures.createCreditApplicationEntity(applicationId));

        var entity1 = createEntity(applicationId);
        repository.save(entity1);

        Thread.sleep(10);

        var entity2 = createEntity(applicationId);
        entity2.setToStatus(ApplicationStatus.IN_PROGRESS);
        entity2.setChangedBy(CHANGED_BY);
        entity2.setFromStatus(VALID_STATUS);
        repository.save(entity2);

        var found = repository.findByApplicationIdOrderByChangedAtAsc(applicationId);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getId()).isEqualTo(entity1.getId());
        assertThat(found.get(1).getId()).isEqualTo(entity2.getId());
    }

    @Test
    @DisplayName("Entity should update correlation ID")
    void update_correlationId() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);
        var id = saved.getId();

        saved.setCorrelationId("updated-correlation");
        repository.save(saved);

        var updated = repository.findById(id).orElseThrow();
        assertThat(updated.getCorrelationId()).isEqualTo("updated-correlation");
    }

    @Test
    @DisplayName("Entity should persist changedAt")
    void persist_changedAt() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getChangedAt()).isNotNull();
    }

    private ApplicationStatusHistoryEntity createEntity(UUID applicationId) {
        return TestFixtures.createStatusHistoryEntity(applicationId);
    }
}
