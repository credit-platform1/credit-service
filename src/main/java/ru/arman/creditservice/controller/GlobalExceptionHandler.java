package ru.arman.creditservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.arman.creditservice.domain.exceptions.ApplicationNotFoundException;
import ru.arman.creditservice.domain.exceptions.IdempotencyConflictException;
import ru.arman.creditservice.domain.exceptions.InvalidStatusTransitionException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://credit-platform.example.com/errors";

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleApplicationNotFound(
            ApplicationNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Credit application not found",
                "APPLICATION_NOT_FOUND",
                exception.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidStatusTransition(
            InvalidStatusTransitionException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Invalid status transition",
                "INVALID_STATUS_TRANSITION",
                exception.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(
            IdempotencyConflictException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Idempotency conflict occurred",
                "IDEMPOTENCY_CONFLICT",
                exception.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingHeader(
            MissingRequestHeaderException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Missing required request header",
                "MISSING_REQUEST_HEADER",
                exception.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        var errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "VALIDATION_ERROR",
                "One or more request fields are invalid",
                errors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception
    ) {
         log.error("Unexpected error", exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred",
                "INTERNAL_ERROR",
                "Please contact support if the issue persists",
                Map.of()
        );
    }

    /**
     * Универсальный хелпер для сборки стандартизированного ResponseEntity<ProblemDetail>
     */
    private ResponseEntity<ProblemDetail> buildResponse(
            HttpStatus status,
            String title,
            String errorCode,
            String detail,
            Map<String, String> invalidFields
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        String kebabCaseCode = errorCode.toLowerCase().replace("_", "-");
        problem.setType(URI.create(ERROR_BASE_URI + kebabCaseCode));

        problem.setProperty("errorCode", errorCode);

        if (!invalidFields.isEmpty()) {
            problem.setProperty("errors", invalidFields);
        }

        return ResponseEntity.status(status).body(problem);
    }
}
