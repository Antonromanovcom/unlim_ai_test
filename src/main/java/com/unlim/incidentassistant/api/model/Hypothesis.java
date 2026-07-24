package com.unlim.incidentassistant.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record Hypothesis(
        @NotBlank
        String title,

        @NotBlank
        String reasoning,

        @NotEmpty
        @Size(min = 2, max = 3)
        List<@NotBlank String> nextSteps
) {
    public Hypothesis {
        nextSteps = nextSteps == null ? null : List.copyOf(nextSteps);
    }
}
