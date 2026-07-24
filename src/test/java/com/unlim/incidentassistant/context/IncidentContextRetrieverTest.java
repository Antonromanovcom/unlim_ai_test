package com.unlim.incidentassistant.context;

import com.unlim.incidentassistant.agent.ParsedIncident;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentContextRetrieverTest {

    @Test
    void selectsOnlyTheMostRelevantIncidents() {
        IncidentKnowledgeBase knowledgeBase = mock(IncidentKnowledgeBase.class);
        when(knowledgeBase.systemDescription()).thenReturn("system");
        when(knowledgeBase.pastIncidents()).thenReturn(List.of(
                incident("INC-101", "paygate", "timeout"),
                incident("INC-102", "database", "cpu"),
                incident("INC-103", "smtp", "email")
        ));
        IncidentContextRetriever retriever = new IncidentContextRetriever(knowledgeBase);

        RetrievedContext result = retriever.retrieve(new ParsedIncident(
                "PayGate timeout affects card payments",
                Set.of("paygate", "timeout", "affects", "card", "payments")
        ));

        assertThat(result.systemDescription()).isEqualTo("system");
        assertThat(result.similarIncidents())
                .extracting(PastIncident::id)
                .containsExactly("INC-101");
    }

    private PastIncident incident(String id, String... keywords) {
        return new PastIncident(id, "category", "description", List.of(keywords));
    }
}
