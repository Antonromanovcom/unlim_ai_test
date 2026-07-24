package com.unlim.incidentassistant.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlim.incidentassistant.api.model.IncidentAnalysis;
import com.unlim.incidentassistant.api.model.ResponseLanguage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class IncidentResponseParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public IncidentResponseParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public IncidentAnalysis parseAndValidate(String output, ResponseLanguage expectedLanguage) {
        if (output == null || output.isBlank()) {
            throw new ModelOutputException("Model returned empty content");
        }

        IncidentAnalysis analysis;
        try {
            analysis = objectMapper.readValue(output, IncidentAnalysis.class);
        } catch (JsonProcessingException exception) {
            throw new ModelOutputException("Model returned invalid JSON or an unexpected structure", exception);
        }

        String violations = validator.validate(analysis).stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(this::formatViolation)
                .collect(Collectors.joining("; "));
        if (!violations.isEmpty()) {
            throw new ModelOutputException("Response validation failed: " + violations);
        }

        validateLanguage(analysis, expectedLanguage);
        return analysis;
    }

    private String formatViolation(ConstraintViolation<IncidentAnalysis> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }

    private void validateLanguage(IncidentAnalysis analysis, ResponseLanguage expectedLanguage) {
        String text = analysis.category() + " " + analysis.summary() + " "
                + analysis.hypotheses().stream()
                .map(hypothesis -> hypothesis.title() + " " + hypothesis.reasoning() + " "
                        + String.join(" ", hypothesis.nextSteps()))
                .collect(Collectors.joining(" "));

        long latin = text.codePoints().filter(character -> character >= 'A' && character <= 'z').count();
        long cyrillic = text.codePoints().filter(character -> character >= 'А' && character <= 'я').count();
        boolean wrongLanguage = expectedLanguage == ResponseLanguage.ENGLISH
                ? cyrillic > latin
                : latin > cyrillic;
        if (wrongLanguage) {
            throw new ModelOutputException("Response language does not match " + expectedLanguage);
        }
    }
}
