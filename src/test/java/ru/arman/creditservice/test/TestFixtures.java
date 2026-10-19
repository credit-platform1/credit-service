package ru.arman.creditservice.test;

import ru.arman.creditservice.domain.dto.*;
import ru.arman.creditservice.domain.entity.CreditApplicationEntity;
import ru.arman.creditservice.domain.entity.ApplicationStatusHistoryEntity;
import ru.arman.creditservice.domain.entity.OutboxEventEntity;
import ru.arman.creditservice.domain.event.ApplicationEventType;
import ru.arman.creditservice.domain.model.ApplicationStatus;
import ru.arman.creditservice.domain.model.EmploymentType;
import ru.arman.creditservice.domain.model.LoanPurpose;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared test fixtures for credit-service tests.
 */
public final class TestFixtures {

    // ─── Customer constants ────────────────────────────────────────────

    public static final String VALID_FIRST_NAME = "John";
    public static final String VALID_LAST_NAME = "Doe";
    public static final String VALID_MIDDLE_NAME = "A";
    public static final LocalDate VALID_BIRTH_DATE = LocalDate.of(1990, 1, 1);
    public static final String VALID_INN = "123456789012";
    public static final String VALID_PHONE = "+79001112233";
    public static final String VALID_EMAIL = "john@test.com";

    // ─── Loan constants ────────────────────────────────────────────────

    public static final BigDecimal VALID_AMOUNT = new BigDecimal("100000");
    public static final int VALID_TERM_MONTHS = 12;
    public static final LoanPurpose VALID_PURPOSE = LoanPurpose.CONSUMER;
    public static final BigDecimal VALID_INCOME = new BigDecimal("50000");
    public static final BigDecimal VALID_EXPENSES = new BigDecimal("10000");
    public static final EmploymentType VALID_EMPLOYMENT = EmploymentType.EMPLOYED;

    // ─── Common constants ──────────────────────────────────────────────

    public static final ApplicationStatus VALID_STATUS = ApplicationStatus.NEW;
    public static final String CHANGED_BY = "SYSTEM";
    public static final String REASON = "Application created";
    public static final String CORRELATION_ID = "test-correlation";
    public static final Instant VALID_TIMESTAMP = Instant.now();

    // ─── DTO constants ─────────────────────────────────────────────────

    public static final String TEST_KEY = "test-key";
    public static final String CORRELATION_ID_HEADER = "corr-1";

    private TestFixtures() {}

    // ─── Request DTO factories ─────────────────────────────────────────

    public static CustomerRequest createCustomerRequest() {
        return new CustomerRequest(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_MIDDLE_NAME,
                VALID_BIRTH_DATE, VALID_INN, VALID_PHONE, VALID_EMAIL);
    }

    public static CustomerRequest createCustomerRequestWithInvalidEmail() {
        return new CustomerRequest(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_MIDDLE_NAME,
                VALID_BIRTH_DATE, VALID_INN, VALID_PHONE, "invalid-email");
    }

    public static CustomerRequest createCustomerRequestWithInvalidPhone() {
        return new CustomerRequest(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_MIDDLE_NAME,
                VALID_BIRTH_DATE, VALID_INN, "+7900", VALID_EMAIL);
    }

    public static LoanRequest createLoanRequest() {
        return new LoanRequest(VALID_AMOUNT, VALID_TERM_MONTHS, VALID_PURPOSE,
                VALID_INCOME, VALID_EXPENSES, VALID_EMPLOYMENT);
    }

    public static LoanRequest createLoanRequestWithInvalidAmount() {
        return new LoanRequest(new BigDecimal("100"), VALID_TERM_MONTHS, VALID_PURPOSE,
                VALID_INCOME, VALID_EXPENSES, VALID_EMPLOYMENT);
    }

    public static CreateCreditApplicationRequest createCreateRequest() {
        return new CreateCreditApplicationRequest(createCustomerRequest(), createLoanRequest());
    }

    public static CancelApplicationRequest createCancelRequest() {
        return new CancelApplicationRequest("Changed my mind");
    }

    public static ChangeApplicationStatusRequest createChangeStatusRequest(ApplicationStatus status) {
        return new ChangeApplicationStatusRequest(status, CHANGED_BY, "Approved by system");
    }

    // ─── Response DTO factories ────────────────────────────────────────

    public static CreditApplicationResponse createApplicationResponse(ApplicationStatus status) {
        return new CreditApplicationResponse(
                UUID.randomUUID().toString(),
                status,
                new CustomerResponse(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_MIDDLE_NAME,
                        VALID_BIRTH_DATE, VALID_PHONE, VALID_EMAIL),
                new LoanResponse(VALID_AMOUNT, VALID_TERM_MONTHS, VALID_PURPOSE,
                        VALID_INCOME, VALID_EXPENSES, VALID_EMPLOYMENT),
                Instant.now(), Instant.now(), 0L
        );
    }

    public static CreateCreditApplicationResponse createCreateResponse() {
        return new CreateCreditApplicationResponse(
                UUID.randomUUID().toString(),
                VALID_STATUS,
                Instant.now(),
                Map.of("self", "/api/v1/credit-applications/test-id")
        );
    }

    public static StatusHistoryResponse createStatusHistoryResponse(UUID applicationId) {
        return new StatusHistoryResponse(
                applicationId.toString(),
                List.of(new StatusHistoryItem(null, VALID_STATUS, CHANGED_BY, REASON, Instant.now()))
        );
    }

    // ─── CreditApplicationEntity factories ─────────────────────────────

    public static CreditApplicationEntity createCreditApplicationEntity() {
        return createCreditApplicationEntity(UUID.randomUUID());
    }

    public static CreditApplicationEntity createCreditApplicationEntity(UUID id) {
        return CreditApplicationEntity.builder()
                .id(id)
                .customerFirstName(VALID_FIRST_NAME)
                .customerLastName(VALID_LAST_NAME)
                .customerMiddleName(VALID_MIDDLE_NAME)
                .customerBirthDate(VALID_BIRTH_DATE)
                .customerInn(VALID_INN)
                .customerPhone(VALID_PHONE)
                .customerEmail(VALID_EMAIL)
                .requestedAmount(VALID_AMOUNT)
                .termMonths(VALID_TERM_MONTHS)
                .purpose(VALID_PURPOSE)
                .monthlyIncome(VALID_INCOME)
                .monthlyExpenses(VALID_EXPENSES)
                .employmentType(VALID_EMPLOYMENT)
                .status(VALID_STATUS)
                .createdAt(VALID_TIMESTAMP)
                .updatedAt(VALID_TIMESTAMP)
                .build();
    }

    // ─── ApplicationStatusHistoryEntity factories ──────────────────────

    public static ApplicationStatusHistoryEntity createStatusHistoryEntity() {
        return createStatusHistoryEntity(UUID.randomUUID());
    }

    public static ApplicationStatusHistoryEntity createStatusHistoryEntity(UUID applicationId) {
        return ApplicationStatusHistoryEntity.builder()
                .applicationId(applicationId)
                .fromStatus(null)
                .toStatus(VALID_STATUS)
                .changedBy(CHANGED_BY)
                .reason(REASON)
                .correlationId(CORRELATION_ID)
                .changedAt(VALID_TIMESTAMP)
                .build();
    }

    // ─── OutboxEventEntity factories ───────────────────────────────────

    public static OutboxEventEntity createOutboxEventEntity() {
        return createOutboxEventEntity(UUID.randomUUID());
    }

    public static OutboxEventEntity createOutboxEventEntity(UUID aggregateId) {
        return OutboxEventEntity.builder()
                .eventType(ApplicationEventType.APPLICATION_CREATED)
                .aggregateId(aggregateId)
                .correlationId(CORRELATION_ID)
                .payload("{\"test\":\"payload\"}")
                .status(OutboxEventEntity.OutboxStatus.PENDING)
                .createdAt(VALID_TIMESTAMP)
                .retryCount(0)
                .build();
    }
}
