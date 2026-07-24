package com.unlim.incidentassistant.api.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Severity {
    LOW,
    MEDIUM,
    HIGH;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }
}
