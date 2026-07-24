package com.unlim.incidentassistant.context;

import com.unlim.incidentassistant.agent.ParsedIncident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Locale;

@Component
public class IncidentContextRetriever {

    private static final int MAX_SIMILAR_INCIDENTS = 2;

    private final IncidentKnowledgeBase knowledgeBase;

    public IncidentContextRetriever(IncidentKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public RetrievedContext retrieve(ParsedIncident incident) {
        var similarIncidents = knowledgeBase.pastIncidents().stream()
                .map(candidate -> new ScoredIncident(candidate, score(candidate, incident)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(ScoredIncident::score).reversed())
                .limit(MAX_SIMILAR_INCIDENTS)
                .map(ScoredIncident::incident)
                .toList();

        return new RetrievedContext(knowledgeBase.systemDescription(), similarIncidents);
    }

    private int score(PastIncident candidate, ParsedIncident incident) {
        return (int) candidate.keywords().stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .filter(keyword -> incident.terms().contains(keyword)
                        || incident.description().toLowerCase(Locale.ROOT).contains(keyword))
                .count();
    }

    private record ScoredIncident(PastIncident incident, int score) {
    }
}
