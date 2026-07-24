package com.unlim.incidentassistant.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IncidentAnalysis(
        @NotBlank
        String category,

        @NotBlank
        String summary,

        @NotNull
        Severity severity,

        @NotEmpty
        @Size(max = 3)
        List<@Valid Hypothesis> hypotheses
) {
    public IncidentAnalysis {
        hypotheses = hypotheses == null ? null : List.copyOf(hypotheses);
    }
}
