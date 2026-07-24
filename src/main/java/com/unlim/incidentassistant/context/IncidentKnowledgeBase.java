package com.unlim.incidentassistant.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class IncidentKnowledgeBase {

    private final String systemDescription;
    private final List<PastIncident> pastIncidents;

    public IncidentKnowledgeBase(
            ObjectMapper objectMapper,
            @Value("classpath:knowledge/system-description.md") Resource systemDescriptionResource,
            @Value("classpath:knowledge/past-incidents.json") Resource pastIncidentsResource
    ) {
        try {
            this.systemDescription = systemDescriptionResource.getContentAsString(StandardCharsets.UTF_8);
            this.pastIncidents = List.copyOf(objectMapper.readValue(
                    pastIncidentsResource.getInputStream(),
                    new TypeReference<List<PastIncident>>() {
                    }
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load incident knowledge base", exception);
        }
    }

    public String systemDescription() {
        return systemDescription;
    }

    public List<PastIncident> pastIncidents() {
        return pastIncidents;
    }
}
