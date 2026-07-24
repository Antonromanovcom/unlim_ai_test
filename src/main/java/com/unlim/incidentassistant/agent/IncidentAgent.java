package com.unlim.incidentassistant.agent;

import com.unlim.incidentassistant.api.model.AnalyzeIncidentRequest;
import com.unlim.incidentassistant.api.model.IncidentAnalysis;
import com.unlim.incidentassistant.context.IncidentContextRetriever;
import com.unlim.incidentassistant.llm.LlmClient;
import com.unlim.incidentassistant.llm.LlmRequest;
import com.unlim.incidentassistant.service.IncidentAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class IncidentAgent implements IncidentAnalyzer {

    private final IncidentInputParser inputParser;
    private final IncidentContextRetriever contextRetriever;
    private final PromptFactory promptFactory;
    private final LlmClient llmClient;
    private final IncidentResponseParser responseParser;

    public IncidentAgent(
            IncidentInputParser inputParser,
            IncidentContextRetriever contextRetriever,
            PromptFactory promptFactory,
            LlmClient llmClient,
            IncidentResponseParser responseParser
    ) {
        this.inputParser = inputParser;
        this.contextRetriever = contextRetriever;
        this.promptFactory = promptFactory;
        this.llmClient = llmClient;
        this.responseParser = responseParser;
    }

    @Override
    public IncidentAnalysis analyze(AnalyzeIncidentRequest request) {
        ParsedIncident incident = inputParser.parse(request.description());
        var context = contextRetriever.retrieve(incident);
        LlmRequest initialRequest = promptFactory.initial(incident, context, request.responseLanguage());
        String firstOutput = llmClient.generate(initialRequest);

        try {
            return responseParser.parseAndValidate(firstOutput, request.responseLanguage());
        } catch (ModelOutputException firstFailure) {
            LlmRequest recoveryRequest = promptFactory.recovery(
                    initialRequest,
                    firstOutput == null ? "" : firstOutput,
                    firstFailure.getMessage()
            );
            String secondOutput = llmClient.generate(recoveryRequest);
            try {
                return responseParser.parseAndValidate(secondOutput, request.responseLanguage());
            } catch (ModelOutputException secondFailure) {
                throw new IncidentAnalysisFailedException(
                        "LLM failed to produce a valid incident analysis after 2 attempts",
                        secondFailure
                );
            }
        }
    }
}
