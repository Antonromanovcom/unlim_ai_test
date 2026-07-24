package com.unlim.incidentassistant.agent;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IncidentInputParser {

    public ParsedIncident parse(String input) {
        String normalized = input.strip().replaceAll("\\s+", " ");
        Set<String> terms = Arrays.stream(normalized.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}-]+"))
                .filter(term -> term.length() >= 3)
                .collect(Collectors.toUnmodifiableSet());
        return new ParsedIncident(normalized, terms);
    }
}
