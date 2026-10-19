package ru.arman.creditservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.arman.creditservice.domain.dto.CreateCreditApplicationRequest;
import ru.arman.creditservice.domain.exceptions.IdempotencyConflictException;
import ru.arman.creditservice.domain.model.ApplicationStatus;
import ru.arman.creditservice.service.CreditApplicationService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.arman.creditservice.test.TestFixtures.*;

@WebMvcTest(CreditApplicationController.class)
class CreditApplicationControllerWebMvcTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreditApplicationService applicationService;

    // ─── Utility ───────────────────────────────────────────────────────

    private String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications - 201 with valid request")
    void create_validRequest_returns201() throws Exception {
        var response = createCreateResponse();

        when(applicationService.create(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/credit-applications")
                        .header("Idempotency-Key", TEST_KEY)
                        .header("X-Correlation-Id", CORRELATION_ID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications - 400 with missing customer")
    void create_missingCustomer_returns400() throws Exception {
        var request = new CreateCreditApplicationRequest(null, createLoanRequest());

        mockMvc.perform(post("/api/v1/credit-applications")
                        .header("Idempotency-Key", TEST_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications - 400 with invalid email")
    void create_invalidEmail_returns400() throws Exception {
        var request = new CreateCreditApplicationRequest(createCustomerRequestWithInvalidEmail(), createLoanRequest());

        mockMvc.perform(post("/api/v1/credit-applications")
                        .header("Idempotency-Key", TEST_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications - 400 with invalid phone")
    void create_invalidPhone_returns400() throws Exception {
        var request = new CreateCreditApplicationRequest(createCustomerRequestWithInvalidPhone(), createLoanRequest());

        mockMvc.perform(post("/api/v1/credit-applications")
                        .header("Idempotency-Key", TEST_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications - 400 with invalid loan amount")
    void create_invalidLoanAmount_returns400() throws Exception {
        var request = new CreateCreditApplicationRequest(createCustomerRequest(), createLoanRequestWithInvalidAmount());

        mockMvc.perform(post("/api/v1/credit-applications")
                        .header("Idempotency-Key", TEST_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications - 400 with missing idempotency key")
    void create_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/credit-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createCreateRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications - 409 with duplicate idempotency key")
    void create_duplicateIdempotencyKey_returns409() throws Exception {
        var idempotencyKey = "duplicate-key";
        when(applicationService.create(any(), any(), any()))
                .thenThrow(new IdempotencyConflictException(idempotencyKey));

        mockMvc.perform(post("/api/v1/credit-applications")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-Id", CORRELATION_ID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createCreateRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    @DisplayName("GET /api/v1/credit-applications/{id} - 404 with error structure")
    void get_invalidId_returns404WithErrorStructure() throws Exception {
        var applicationId = UUID.randomUUID();
        when(applicationService.get(applicationId))
                .thenThrow(new ru.arman.creditservice.domain.exceptions.ApplicationNotFoundException(applicationId));

        mockMvc.perform(get("/api/v1/credit-applications/{id}", applicationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Credit application not found"))
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Credit application not found: " + applicationId));
    }

    @Test
    @DisplayName("GET /api/v1/credit-applications/{id} - 200 with valid id")
    void get_validId_returns200() throws Exception {
        var applicationId = UUID.randomUUID();
        when(applicationService.get(applicationId)).thenReturn(createApplicationResponse(ApplicationStatus.NEW));

        mockMvc.perform(get("/api/v1/credit-applications/{id}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @DisplayName("GET /api/v1/credit-applications/{id} - 404 with invalid id")
    void get_invalidId_returns404() throws Exception {
        var applicationId = UUID.randomUUID();
        when(applicationService.get(applicationId))
                .thenThrow(new ru.arman.creditservice.domain.exceptions.ApplicationNotFoundException(applicationId));

        mockMvc.perform(get("/api/v1/credit-applications/{id}", applicationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/credit-applications/{id}/status-history - 200 with valid id")
    void getStatusHistory_validId_returns200() throws Exception {
        var applicationId = UUID.randomUUID();
        var response = createStatusHistoryResponse(applicationId);

        when(applicationService.getStatusHistory(applicationId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/credit-applications/{id}/status-history", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.items[0].toStatus").value("NEW"));
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications/{id}/cancel - 200 with valid request")
    void cancel_validRequest_returns200() throws Exception {
        var applicationId = UUID.randomUUID();
        when(applicationService.cancel(any(), any(), any()))
                .thenReturn(createApplicationResponse(ApplicationStatus.CANCELLED));

        mockMvc.perform(post("/api/v1/credit-applications/{id}/cancel", applicationId)
                        .header("X-Correlation-Id", CORRELATION_ID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createCancelRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/v1/credit-applications/{id}/cancel - 409 with invalid transition")
    void cancel_invalidTransition_returns409() throws Exception {
        var applicationId = UUID.randomUUID();
        when(applicationService.cancel(any(), any(), any()))
                .thenThrow(new ru.arman.creditservice.domain.exceptions.InvalidStatusTransitionException(
                        ApplicationStatus.APPROVED, ApplicationStatus.CANCELLED));

        mockMvc.perform(post("/api/v1/credit-applications/{id}/cancel", applicationId)
                        .header("X-Correlation-Id", CORRELATION_ID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createCancelRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("PATCH /api/v1/credit-applications/{id}/status - 200 with valid request")
    void changeStatus_validRequest_returns200() throws Exception {
        var applicationId = UUID.randomUUID();
        when(applicationService.changeStatus(any(), any(), any()))
                .thenReturn(createApplicationResponse(ApplicationStatus.APPROVED));

        mockMvc.perform(patch("/api/v1/credit-applications/{id}/status", applicationId)
                        .header("X-Correlation-Id", CORRELATION_ID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createChangeStatusRequest(ApplicationStatus.APPROVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/credit-applications/{id}/status - 409 with invalid transition")
    void changeStatus_invalidTransition_returns409() throws Exception {
        var applicationId = UUID.randomUUID();
        when(applicationService.changeStatus(any(), any(), any()))
                .thenThrow(new ru.arman.creditservice.domain.exceptions.InvalidStatusTransitionException(
                        ApplicationStatus.NEW, ApplicationStatus.APPROVED));

        mockMvc.perform(patch("/api/v1/credit-applications/{id}/status", applicationId)
                        .header("X-Correlation-Id", CORRELATION_ID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createChangeStatusRequest(ApplicationStatus.APPROVED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("PATCH /api/v1/credit-applications/{id}/status - 400 with missing status")
    void changeStatus_missingStatus_returns400() throws Exception {
        var applicationId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/credit-applications/{id}/status", applicationId)
                        .header("X-Correlation-Id", CORRELATION_ID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createChangeStatusRequest(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
