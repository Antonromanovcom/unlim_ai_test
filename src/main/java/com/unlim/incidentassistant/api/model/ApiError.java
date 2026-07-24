package com.unlim.incidentassistant.api.model;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> violations
) {
    public ApiError {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }
}
