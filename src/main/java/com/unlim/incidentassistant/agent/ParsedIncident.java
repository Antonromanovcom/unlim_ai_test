package com.unlim.incidentassistant.agent;

import java.util.Set;

public record ParsedIncident(
        String description,
        Set<String> terms
) {
    public ParsedIncident {
        terms = Set.copyOf(terms);
    }
}
