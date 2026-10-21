package ru.arman.creditservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.arman.creditservice.domain.dto.*;
import ru.arman.creditservice.domain.entity.ApplicationStatusHistoryEntity;
import ru.arman.creditservice.domain.entity.CreditApplicationEntity;
import ru.arman.creditservice.domain.entity.IdempotencyKeyEntity;
import ru.arman.creditservice.domain.entity.OutboxEventEntity;
import ru.arman.creditservice.domain.event.ApplicationCancelledEvent;
import ru.arman.creditservice.domain.event.ApplicationCreatedEvent;
import ru.arman.creditservice.domain.event.ApplicationEventType;
import ru.arman.creditservice.domain.event.ApplicationStatusChangedEvent;
import ru.arman.creditservice.domain.exceptions.ApplicationNotFoundException;
import ru.arman.creditservice.domain.exceptions.IdempotencyConflictException;
import ru.arman.creditservice.domain.exceptions.InvalidStatusTransitionException;
import ru.arman.creditservice.domain.model.ApplicationStatus;
import ru.arman.creditservice.infrastructure.json.JsonUtils;
import ru.arman.creditservice.repository.ApplicationStatusHistoryRepository;
import ru.arman.creditservice.repository.CreditApplicationRepository;
import ru.arman.creditservice.repository.IdempotencyKeyRepository;
import ru.arman.creditservice.repository.OutboxEventRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditApplicationService {
    private final CreditApplicationRepository applicationRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final OutboxEventRepository outboxRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final RequestContext requestContext;

    @Transactional
    public CreateCreditApplicationResponse create(
            String idempotencyKey,
            String correlationId,
            CreateCreditApplicationRequest request
    ) {
        if (correlationId == null) correlationId = requestContext.getCorrelationId();
        log.info("Starting creation of credit application");
        var requestHash = sha256(request);

        var existingIdempotency = idempotencyRepository.findById(idempotencyKey);
        if (existingIdempotency.isPresent()) {
            log.info("Idempotency hit during creation of credit application");
            var saved = existingIdempotency.get();

            if (!saved.getRequestHash().equals(requestHash)) {
                log.warn("Idempotency conflict: idempotencyKey={}", idempotencyKey);
                throw new IdempotencyConflictException(idempotencyKey);
            }

            var application = applicationRepository.findById(saved.getApplicationId())
                    .orElseThrow(() -> new ApplicationNotFoundException(saved.getApplicationId()));

            return toCreateResponse(application);
        }

        var application = CreditApplicationEntity.builder()
                .id(UUID.randomUUID())
                .customerFirstName(request.customer().firstName())
                .customerLastName(request.customer().lastName())
                .customerMiddleName(request.customer().middleName())
                .customerBirthDate(request.customer().birthDate())
                .customerInn(request.customer().inn())
                .customerPhone(request.customer().phone())
                .customerEmail(request.customer().email())
                .requestedAmount(request.loan().requestedAmount())
                .termMonths(request.loan().termMonths())
                .purpose(request.loan().purpose())
                .monthlyIncome(request.loan().monthlyIncome())
                .monthlyExpenses(request.loan().monthlyExpenses())
                .employmentType(request.loan().employmentType())
                .status(ApplicationStatus.NEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        applicationRepository.save(application);
        log.info("Application saved: applicationId={}", application.getId());

        historyRepository.save(
                ApplicationStatusHistoryEntity.builder()
                        .applicationId(application.getId())
                        .fromStatus(null)
                        .toStatus(ApplicationStatus.NEW)
                        .changedBy("SYSTEM")
                        .reason("Application created")
                        .correlationId(correlationId)
                        .changedAt(Instant.now())
                        .build()
        );

        var event = new ApplicationCreatedEvent(
                UUID.randomUUID(),
                application.getId(),
                correlationId,
                Instant.now(),
                toCustomerPayload(application),
                toLoanPayload(application)
        );

        outboxRepository.save(
                OutboxEventEntity.builder()
                        .eventType(ApplicationEventType.APPLICATION_CREATED)
                        .aggregateId(application.getId())
                        .correlationId(correlationId)
                        .payload(JsonUtils.toJson(event))
                        .status(OutboxEventEntity.OutboxStatus.PENDING)
                        .createdAt(Instant.now())
                        .retryCount(0)
                        .build()
        );

        try {
            idempotencyRepository.save(
                    IdempotencyKeyEntity.builder()
                            .idempotencyKey(idempotencyKey)
                            .applicationId(application.getId())
                            .requestHash(requestHash)
                            .createdAt(Instant.now())
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            log.warn("Idempotency conflict on save: idempotencyKey={}", idempotencyKey);
            throw new IdempotencyConflictException(idempotencyKey);
        }

        return toCreateResponse(application);
    }

    @Transactional(readOnly = true)
    public CreditApplicationResponse get(UUID applicationId) {
        log.info("Getting application with applicationId={}", applicationId);
        return applicationRepository.findById(applicationId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    @Transactional
    public CreditApplicationResponse cancel(
            UUID applicationId,
            CancelApplicationRequest request,
            String correlationId
    ) {
        log.info("Cancelling credit application with applicationId={}", applicationId);
        if (correlationId == null) correlationId = requestContext.getCorrelationId();

        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        var previousStatus = application.getStatus();

        if (!StatusTransitionPolicy.canTransition(
                previousStatus,
                ApplicationStatus.CANCELLED
        )) {
            log.warn("Invalid transition: applicationId={}, from={}, to={}", applicationId, previousStatus, ApplicationStatus.CANCELLED);
            throw new InvalidStatusTransitionException(
                    previousStatus,
                    ApplicationStatus.CANCELLED
            );
        }

        application.setStatus(ApplicationStatus.CANCELLED);
        application.setUpdatedAt(Instant.now());
        log.info("Application cancelled: applicationId={}", applicationId);

        historyRepository.save(
                ApplicationStatusHistoryEntity.builder()
                        .applicationId(application.getId())
                        .fromStatus(previousStatus)
                        .toStatus(ApplicationStatus.CANCELLED)
                        .changedBy("CUSTOMER")
                        .reason(request.reason())
                        .correlationId(correlationId)
                        .changedAt(Instant.now())
                        .build()
        );

        var event = new ApplicationCancelledEvent(
                UUID.randomUUID(),
                application.getId(),
                correlationId,
                Instant.now(),
                request.reason()
        );

        outboxRepository.save(
                OutboxEventEntity.builder()
                        .eventType(ApplicationEventType.APPLICATION_CANCELLED)
                        .aggregateId(application.getId())
                        .correlationId(correlationId)
                        .payload(JsonUtils.toJson(event))
                        .status(OutboxEventEntity.OutboxStatus.PENDING)
                        .createdAt(Instant.now())
                        .retryCount(0)
                        .build()
        );

        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public StatusHistoryResponse getStatusHistory(UUID applicationId) {
        log.info("[GetStatusHistory service] applicationId={}", applicationId);
        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

        return new StatusHistoryResponse(
                applicationId.toString(),
                historyRepository.findByApplicationIdOrderByChangedAtAsc(applicationId)
                        .stream()
                        .map(this::toHistoryItem)
                        .toList());
    }

    @Transactional
    public CreditApplicationResponse changeStatus(
            UUID applicationId,
            ChangeApplicationStatusRequest request,
            String correlationId
    ) {
        log.info("Changing status applicationId={}, status={}", applicationId, request.status());
        if (correlationId == null) correlationId = requestContext.getCorrelationId();
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        var currentStatus = application.getStatus();
        var targetStatus = request.status();

        if (!StatusTransitionPolicy.canTransition(currentStatus, targetStatus)) {
            log.warn("Invalid transition: applicationId={}, from={}, to={}", applicationId, currentStatus, targetStatus);
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        application.setStatus(targetStatus);
        application.setUpdatedAt(Instant.now());
        log.info("Status changed: applicationId={}, from={}, to={}", applicationId, currentStatus, targetStatus);

        historyRepository.save(
                ApplicationStatusHistoryEntity.builder()
                        .applicationId(application.getId())
                        .fromStatus(currentStatus)
                        .toStatus(targetStatus)
                        .changedBy(request.changedBy())
                        .reason(request.reason())
                        .correlationId(correlationId)
                        .changedAt(Instant.now())
                        .build()
        );

        var event = new ApplicationStatusChangedEvent(
                UUID.randomUUID(),
                application.getId(),
                correlationId,
                Instant.now(),
                currentStatus,
                targetStatus,
                request.changedBy(),
                request.reason()
        );

        outboxRepository.save(
                OutboxEventEntity.builder()
                        .eventType(ApplicationEventType.APPLICATION_STATUS_CHANGED)
                        .aggregateId(application.getId())
                        .correlationId(correlationId)
                        .payload(JsonUtils.toJson(event))
                        .status(OutboxEventEntity.OutboxStatus.PENDING)
                        .createdAt(Instant.now())
                        .retryCount(0)
                        .build()
        );

        return toResponse(application);
    }

    private StatusHistoryItem toHistoryItem(ApplicationStatusHistoryEntity history) {
        return new StatusHistoryItem(
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getReason(),
                history.getChangedAt()
        );
    }

    private String sha256(Object value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(
                    JsonUtils.toJson(value).getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot calculate request hash", e);
        }
    }

    private CreateCreditApplicationResponse toCreateResponse(
            CreditApplicationEntity application
    ) {
        return new CreateCreditApplicationResponse(
                application.getId().toString(),
                application.getStatus(),
                application.getCreatedAt(),
                Map.of(
                        "self",
                        "/api/v1/credit-applications/" + application.getId()
                )
        );
    }

    private CreditApplicationResponse toResponse(CreditApplicationEntity application) {
        return new CreditApplicationResponse(
                application.getId().toString(),
                application.getStatus(),
                new CustomerResponse(
                        application.getCustomerFirstName(),
                        application.getCustomerLastName(),
                        application.getCustomerMiddleName(),
                        application.getCustomerBirthDate(),
                        application.getCustomerPhone(),
                        application.getCustomerEmail()
                ),
                new LoanResponse(
                        application.getRequestedAmount(),
                        application.getTermMonths(),
                        application.getPurpose(),
                        application.getMonthlyIncome(),
                        application.getMonthlyExpenses(),
                        application.getEmploymentType()
                ),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                application.getVersion()
        );
    }

    private ApplicationCreatedEvent.CustomerPayload toCustomerPayload(
            CreditApplicationEntity application
    ) {
        return new ApplicationCreatedEvent.CustomerPayload(
                application.getCustomerFirstName(),
                application.getCustomerLastName(),
                application.getCustomerMiddleName(),
                application.getCustomerBirthDate().toString(),
                application.getCustomerInn(),
                application.getCustomerPhone(),
                application.getCustomerEmail()
        );
    }

    private ApplicationCreatedEvent.LoanPayload toLoanPayload(
            CreditApplicationEntity application
    ) {
        return new ApplicationCreatedEvent.LoanPayload(
                application.getRequestedAmount(),
                application.getTermMonths(),
                application.getPurpose().name(),
                application.getMonthlyIncome(),
                application.getMonthlyExpenses(),
                application.getEmploymentType().name()
        );
    }
}