package com.unlim.incidentassistant.api;

import com.unlim.incidentassistant.api.model.ApiError;
import com.unlim.incidentassistant.api.model.FieldViolation;
import com.unlim.incidentassistant.agent.IncidentAnalysisFailedException;
import com.unlim.incidentassistant.llm.LlmUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ApiError> handleLlmUnavailable(
            LlmUnavailableException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(IncidentAnalysisFailedException.class)
    public ResponseEntity<ApiError> handleAnalysisFailure(
            IncidentAnalysisFailedException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldViolation> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                violations
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed",
                request,
                List.of()
        );
    }

    private ResponseEntity<ApiError> errorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<FieldViolation> violations
    ) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                violations
        );
        return ResponseEntity.status(status).body(body);
    }
}
