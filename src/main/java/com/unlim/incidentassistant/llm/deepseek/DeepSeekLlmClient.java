package com.unlim.incidentassistant.llm.deepseek;

import com.unlim.incidentassistant.llm.LlmClient;
import com.unlim.incidentassistant.llm.LlmMessage;
import com.unlim.incidentassistant.llm.LlmRequest;
import com.unlim.incidentassistant.llm.LlmUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Locale;

@Component
public class DeepSeekLlmClient implements LlmClient {

    private final RestClient restClient;
    private final DeepSeekProperties properties;

    public DeepSeekLlmClient(RestClient deepSeekRestClient, DeepSeekProperties properties) {
        this.restClient = deepSeekRestClient;
        this.properties = properties;
    }

    @Override
    public String generate(LlmRequest request) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new LlmUnavailableException("DeepSeek API key is not configured");
        }

        DeepSeekApiRequest apiRequest = new DeepSeekApiRequest(
                properties.model(),
                toMessages(request.messages()),
                new DeepSeekApiRequest.ResponseFormat("json_object"),
                new DeepSeekApiRequest.Thinking("disabled"),
                0.1,
                properties.maxTokens()
        );

        try {
            DeepSeekApiResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(apiRequest)
                    .retrieve()
                    .body(DeepSeekApiResponse.class);
            return extractContent(response);
        } catch (RestClientResponseException exception) {
            throw mapHttpError(exception);
        } catch (ResourceAccessException exception) {
            throw new LlmUnavailableException("DeepSeek request timed out or could not connect", exception);
        }
    }

    private List<DeepSeekApiRequest.DeepSeekMessage> toMessages(List<LlmMessage> messages) {
        return messages.stream()
                .map(message -> new DeepSeekApiRequest.DeepSeekMessage(
                        message.role().name().toLowerCase(Locale.ROOT),
                        message.content()
                ))
                .toList();
    }

    private String extractContent(DeepSeekApiResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null) {
            throw new LlmUnavailableException("DeepSeek returned an unexpected response");
        }
        return response.choices().getFirst().message().content();
    }

    private LlmUnavailableException mapHttpError(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        String message = switch (status) {
            case 401, 403 -> "DeepSeek rejected the API credentials";
            case 429 -> "DeepSeek rate limit exceeded";
            default -> status >= 500
                    ? "DeepSeek service is temporarily unavailable"
                    : "DeepSeek request was rejected with HTTP " + status;
        };
        return new LlmUnavailableException(message, exception);
    }
}
