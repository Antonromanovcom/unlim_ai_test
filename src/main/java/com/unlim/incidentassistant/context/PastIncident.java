package com.unlim.incidentassistant.context;

import java.util.List;

public record PastIncident(
        String id,
        String category,
        String description,
        List<String> keywords
) {
    public PastIncident {
        keywords = List.copyOf(keywords);
    }
}
