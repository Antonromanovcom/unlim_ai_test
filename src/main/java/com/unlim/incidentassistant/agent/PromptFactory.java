package com.unlim.incidentassistant.agent;

import com.unlim.incidentassistant.api.model.ResponseLanguage;
import com.unlim.incidentassistant.context.PastIncident;
import com.unlim.incidentassistant.context.RetrievedContext;
import com.unlim.incidentassistant.llm.LlmMessage;
import com.unlim.incidentassistant.llm.LlmRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.unlim.incidentassistant.llm.LlmMessage.Role.ASSISTANT;
import static com.unlim.incidentassistant.llm.LlmMessage.Role.SYSTEM;
import static com.unlim.incidentassistant.llm.LlmMessage.Role.USER;

@Component
public class PromptFactory {

    private static final String JSON_CONTRACT = """
            {
              "category": "string",
              "summary": "string",
              "severity": "low|medium|high",
              "hypotheses": [
                {
                  "title": "string",
                  "reasoning": "string",
                  "next_steps": ["string", "string"]
                }
              ]
            }
            """;

    public LlmRequest initial(
            ParsedIncident incident,
            RetrievedContext context,
            ResponseLanguage language
    ) {
        return new LlmRequest(List.of(
                new LlmMessage(SYSTEM, systemPrompt(context, language)),
                new LlmMessage(USER, "Incident description:\n" + incident.description())
        ));
    }

    public LlmRequest recovery(
            LlmRequest initialRequest,
            String invalidOutput,
            String validationError
    ) {
        List<LlmMessage> messages = new ArrayList<>(initialRequest.messages());
        messages.add(new LlmMessage(ASSISTANT, invalidOutput));
        messages.add(new LlmMessage(USER, """
                Your previous answer was rejected: %s
                Return a corrected JSON object only. Do not add markdown or explanations.
                """.formatted(validationError)));
        return new LlmRequest(messages);
    }

    private String systemPrompt(RetrievedContext context, ResponseLanguage language) {
        String examples = context.similarIncidents().isEmpty()
                ? "No sufficiently similar past incident was found."
                : context.similarIncidents().stream()
                        .map(this::formatIncident)
                        .reduce((left, right) -> left + "\n---\n" + right)
                        .orElseThrow();

        return """
                You are an incident triage agent for on-call engineers.
                Analyze only the evidence provided. Do not invent observed facts.
                Return valid JSON matching the contract below and no other text.
                Use %s for every human-readable value.
                Provide 1-3 hypotheses and exactly 2-3 concrete next steps for each hypothesis.

                JSON contract:
                %s

                System description:
                %s

                Relevant past incidents:
                %s
                """.formatted(language.name().toLowerCase(), JSON_CONTRACT,
                context.systemDescription(), examples);
    }

    private String formatIncident(PastIncident incident) {
        return "[%s] Category: %s\n%s".formatted(
                incident.id(),
                incident.category(),
                incident.description()
        );
    }
}
