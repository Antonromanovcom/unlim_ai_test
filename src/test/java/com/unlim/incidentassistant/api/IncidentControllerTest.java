package com.unlim.incidentassistant.api;

import com.unlim.incidentassistant.api.model.Hypothesis;
import com.unlim.incidentassistant.api.model.IncidentAnalysis;
import com.unlim.incidentassistant.api.model.Severity;
import com.unlim.incidentassistant.service.IncidentAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentAnalyzer incidentAnalyzer;

    @Test
    void returnsStructuredIncidentAnalysis() throws Exception {
        IncidentAnalysis analysis = new IncidentAnalysis(
                "External payment provider issue",
                "Card payments fail because PayGate calls time out.",
                Severity.HIGH,
                List.of(new Hypothesis(
                        "PayGate degradation",
                        "Timeouts are isolated to PayGate.",
                        List.of(
                                "Check the PayGate status page.",
                                "Compare provider latency metrics."
                        )
                ))
        );
        when(incidentAnalyzer.analyze(any())).thenReturn(analysis);

        mockMvc.perform(post("/api/v1/incidents/analyze")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Customers cannot pay by card because PayGate calls time out.",
                                  "response_language": "ENGLISH"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("External payment provider issue"))
                .andExpect(jsonPath("$.severity").value("high"))
                .andExpect(jsonPath("$.hypotheses[0].next_steps.length()").value(2));

        verify(incidentAnalyzer).analyze(any());
    }

    @Test
    void rejectsInvalidRequestBeforeCallingAgent() throws Exception {
        mockMvc.perform(post("/api/v1/incidents/analyze")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "short",
                                  "response_language": "ENGLISH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.violations[0].field").value("description"));
    }

    @Test
    void rejectsUnknownFields() throws Exception {
        mockMvc.perform(post("/api/v1/incidents/analyze")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Customers cannot complete card payments.",
                                  "response_language": "ENGLISH",
                                  "unexpected": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }
}
