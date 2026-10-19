package ru.arman.creditservice.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.arman.creditservice.domain.model.ApplicationStatus;
import ru.arman.creditservice.repository.CreditApplicationRepository;
import ru.arman.creditservice.test.TestFixtures;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.arman.creditservice.test.TestFixtures.*;

@Testcontainers
@DataJpaTest
class CreditApplicationEntityDataJpaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CreditApplicationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Entity should auto-generate UUID on persist")
    void persist_autoGeneratesUUID() {
        var entity = createEntity(null);
        var saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("Entity should auto-set updatedAt on persist")
    void persist_autoSetsUpdatedAt() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Entity should auto-increment version on update")
    void update_incrementsVersion() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);
        entityManager.flush();
        entityManager.clear();

        var fresh = repository.findById(saved.getId()).orElseThrow();
        var initialVersion = fresh.getVersion();

        fresh.setStatus(ApplicationStatus.IN_PROGRESS);
        entityManager.flush();

        var updated = repository.findById(fresh.getId()).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(initialVersion + 1);
    }

    @Test
    @DisplayName("Entity should auto-update updatedAt on update")
    void update_autoUpdatesUpdatedAt() throws InterruptedException {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);
        entityManager.flush();
        entityManager.clear();

        var initialUpdatedAt = repository.findById(saved.getId()).orElseThrow().getUpdatedAt();

        Thread.sleep(10);

        var fresh = repository.findById(saved.getId()).orElseThrow();
        fresh.setStatus(ApplicationStatus.IN_PROGRESS);
        entityManager.flush();

        var updated = repository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @Test
    @DisplayName("Entity should persist all fields correctly")
    void persist_allFieldsPersisted() {
        var id = UUID.randomUUID();
        var entity = createEntity(id);
        var saved = repository.save(entity);

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getCustomerFirstName()).isEqualTo(VALID_FIRST_NAME);
        assertThat(saved.getCustomerLastName()).isEqualTo(VALID_LAST_NAME);
        assertThat(saved.getCustomerMiddleName()).isEqualTo(VALID_MIDDLE_NAME);
        assertThat(saved.getCustomerBirthDate()).isEqualTo(VALID_BIRTH_DATE);
        assertThat(saved.getCustomerInn()).isEqualTo(VALID_INN);
        assertThat(saved.getCustomerPhone()).isEqualTo(VALID_PHONE);
        assertThat(saved.getCustomerEmail()).isEqualTo(VALID_EMAIL);
        assertThat(saved.getRequestedAmount()).isEqualByComparingTo(VALID_AMOUNT);
        assertThat(saved.getTermMonths()).isEqualTo(VALID_TERM_MONTHS);
        assertThat(saved.getPurpose()).isEqualTo(VALID_PURPOSE);
        assertThat(saved.getMonthlyIncome()).isEqualByComparingTo(VALID_INCOME);
        assertThat(saved.getMonthlyExpenses()).isEqualByComparingTo(VALID_EXPENSES);
        assertThat(saved.getEmploymentType()).isEqualTo(VALID_EMPLOYMENT);
        assertThat(saved.getStatus()).isEqualTo(VALID_STATUS);
    }

    @Test
    @DisplayName("Entity should find by ID")
    void findByExists() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCustomerFirstName()).isEqualTo(VALID_FIRST_NAME);
    }

    @Test
    @DisplayName("Entity should not exist after removal")
    void findByReturnsEmpty() {
        var entity = createEntity(UUID.randomUUID());
        var saved = repository.save(entity);
        var id = saved.getId();

        repository.delete(saved);

        assertThat(repository.findById(id)).isEmpty();
    }

    private CreditApplicationEntity createEntity(UUID id) {
        return TestFixtures.createCreditApplicationEntity(id);
    }
}