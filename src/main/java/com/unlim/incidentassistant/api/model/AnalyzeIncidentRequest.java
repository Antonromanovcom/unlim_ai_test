package com.unlim.incidentassistant.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyzeIncidentRequest(
        @NotBlank
        @Size(min = 10, max = 10_000)
        String description,

        ResponseLanguage responseLanguage
) {
    public AnalyzeIncidentRequest {
        responseLanguage = responseLanguage == null ? ResponseLanguage.ENGLISH : responseLanguage;
    }
}
