package com.unlim.incidentassistant.api.model;

public record FieldViolation(
        String field,
        String message
) {
}
