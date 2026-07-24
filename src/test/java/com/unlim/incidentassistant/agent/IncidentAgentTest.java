package com.unlim.incidentassistant.agent;

import com.unlim.incidentassistant.api.model.AnalyzeIncidentRequest;
import com.unlim.incidentassistant.api.model.Hypothesis;
import com.unlim.incidentassistant.api.model.IncidentAnalysis;
import com.unlim.incidentassistant.api.model.ResponseLanguage;
import com.unlim.incidentassistant.api.model.Severity;
import com.unlim.incidentassistant.context.IncidentContextRetriever;
import com.unlim.incidentassistant.context.RetrievedContext;
import com.unlim.incidentassistant.llm.LlmClient;
import com.unlim.incidentassistant.llm.LlmMessage;
import com.unlim.incidentassistant.llm.LlmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.unlim.incidentassistant.llm.LlmMessage.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentAgentTest {

    @Mock
    private IncidentInputParser inputParser;
    @Mock
    private IncidentContextRetriever contextRetriever;
    @Mock
    private PromptFactory promptFactory;
    @Mock
    private LlmClient llmClient;
    @Mock
    private IncidentResponseParser responseParser;

    private IncidentAgent agent;

    @BeforeEach
    void setUp() {
        agent = new IncidentAgent(
                inputParser,
                contextRetriever,
                promptFactory,
                llmClient,
                responseParser
        );
    }

    @Test
    void returnsFirstValidModelOutput() {
        Scenario scenario = scenario();
        IncidentAnalysis expected = validAnalysis();
        when(llmClient.generate(scenario.initialRequest())).thenReturn("valid json");
        when(responseParser.parseAndValidate("valid json", ResponseLanguage.ENGLISH))
                .thenReturn(expected);

        IncidentAnalysis actual = agent.analyze(scenario.request());

        assertThat(actual).isSameAs(expected);
        verifyNoMoreInteractions(llmClient);
    }

    @Test
    void retriesWithValidationFeedback() {
        Scenario scenario = scenario();
        LlmRequest recoveryRequest = new LlmRequest(List.of(new LlmMessage(USER, "repair")));
        IncidentAnalysis expected = validAnalysis();
        when(llmClient.generate(scenario.initialRequest())).thenReturn("broken");
        when(responseParser.parseAndValidate("broken", ResponseLanguage.ENGLISH))
                .thenThrow(new ModelOutputException("invalid JSON"));
        when(promptFactory.recovery(scenario.initialRequest(), "broken", "invalid JSON"))
                .thenReturn(recoveryRequest);
        when(llmClient.generate(recoveryRequest)).thenReturn("fixed");
        when(responseParser.parseAndValidate("fixed", ResponseLanguage.ENGLISH))
                .thenReturn(expected);

        assertThat(agent.analyze(scenario.request())).isSameAs(expected);

        InOrder order = inOrder(llmClient);
        order.verify(llmClient).generate(scenario.initialRequest());
        order.verify(llmClient).generate(recoveryRequest);
    }

    @Test
    void failsAfterSecondInvalidOutput() {
        Scenario scenario = scenario();
        LlmRequest recoveryRequest = new LlmRequest(List.of(new LlmMessage(USER, "repair")));
        when(llmClient.generate(scenario.initialRequest())).thenReturn("broken");
        when(responseParser.parseAndValidate("broken", ResponseLanguage.ENGLISH))
                .thenThrow(new ModelOutputException("invalid JSON"));
        when(promptFactory.recovery(scenario.initialRequest(), "broken", "invalid JSON"))
                .thenReturn(recoveryRequest);
        when(llmClient.generate(recoveryRequest)).thenReturn("still broken");
        when(responseParser.parseAndValidate("still broken", ResponseLanguage.ENGLISH))
                .thenThrow(new ModelOutputException("invalid structure"));

        assertThatThrownBy(() -> agent.analyze(scenario.request()))
                .isInstanceOf(IncidentAnalysisFailedException.class)
                .hasMessageContaining("after 2 attempts");
    }

    private Scenario scenario() {
        AnalyzeIncidentRequest request = new AnalyzeIncidentRequest(
                "Customers cannot pay by card because PayGate calls time out.",
                ResponseLanguage.ENGLISH
        );
        ParsedIncident parsed = new ParsedIncident(request.description(), Set.of("paygate"));
        RetrievedContext context = new RetrievedContext("system", List.of());
        LlmRequest initialRequest = new LlmRequest(List.of(new LlmMessage(USER, "analyze")));
        when(inputParser.parse(request.description())).thenReturn(parsed);
        when(contextRetriever.retrieve(parsed)).thenReturn(context);
        when(promptFactory.initial(parsed, context, ResponseLanguage.ENGLISH))
                .thenReturn(initialRequest);
        return new Scenario(request, initialRequest);
    }

    private IncidentAnalysis validAnalysis() {
        return new IncidentAnalysis(
                "External payment provider issue",
                "PayGate calls time out.",
                Severity.HIGH,
                List.of(new Hypothesis(
                        "Provider degradation",
                        "Only PayGate calls fail.",
                        List.of("Check provider status.", "Check latency metrics.")
                ))
        );
    }

    private record Scenario(
            AnalyzeIncidentRequest request,
            LlmRequest initialRequest
    ) {
    }
}
