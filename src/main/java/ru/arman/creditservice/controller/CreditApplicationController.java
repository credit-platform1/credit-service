package ru.arman.creditservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import ru.arman.creditservice.domain.dto.*;
import ru.arman.creditservice.service.CreditApplicationService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/credit-applications")
@RequiredArgsConstructor
public class CreditApplicationController {
    private final CreditApplicationService applicationService;

    @Operation(
            summary = "Create credit application",
            description = "Creates a new credit application and publishes an application.created event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    @PostMapping
    public ResponseEntity<CreateCreditApplicationResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody CreateCreditApplicationRequest request
    ) {
        log.info("Creating credit application correlationId={}, idempotencyKey={}", correlationId, idempotencyKey);
        var response = applicationService.create(idempotencyKey, correlationId, request);

        return ResponseEntity
                .created(UriComponentsBuilder
                        .fromPath("/api/v1/credit-applications/{id}")
                        .buildAndExpand(response.applicationId())
                        .toUri())
                .body(response);
    }

    @Operation(
            summary = "Get credit application",
            description = "Returns a credit application by its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @GetMapping("/{applicationId}")
    public CreditApplicationResponse get(@PathVariable UUID applicationId) {
        log.info("Getting credit application by applicationId={}", applicationId);
        return applicationService.get(applicationId);
    }

    @Operation(
            summary = "Get status history",
            description = "Returns the full status change history for a credit application."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History found"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @GetMapping("/{applicationId}/status-history")
    public StatusHistoryResponse getStatusHistory(@PathVariable UUID applicationId) {
        log.info("Getting status history by applicationId={}", applicationId);
        return applicationService.getStatusHistory(applicationId);
    }

    @Operation(
            summary = "Cancel credit application",
            description = "Cancels a credit application (transitions to CANCELLED). Only allowed for NEW or IN_PROGRESS applications."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PostMapping("/{applicationId}/cancel")
    public CreditApplicationResponse cancel(
            @PathVariable UUID applicationId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody CancelApplicationRequest request
    ) {
        log.info("Cancelling credit application with applicationId={}, correlationId={}", applicationId, correlationId);
        return applicationService.cancel(applicationId, request, correlationId);
    }

    @Operation(
            summary = "Change application status",
            description = "Changes the status of a credit application (e.g. IN_PROGRESS, MANUAL_REVIEW, APPROVED, DECLINED). Validates status transition rules."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PatchMapping("/{applicationId}/status")
    public CreditApplicationResponse changeStatus(
            @PathVariable UUID applicationId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody ChangeApplicationStatusRequest request
    ) {
        log.info("Changing status of credit application, applicationId={}, correlationId={}, status={}", applicationId, correlationId, request.status());
        return applicationService.changeStatus(
                applicationId,
                request,
                correlationId
        );
    }
}
