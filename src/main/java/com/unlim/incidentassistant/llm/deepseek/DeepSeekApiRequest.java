package com.unlim.incidentassistant.llm.deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record DeepSeekApiRequest(
        String model,
        List<DeepSeekMessage> messages,
        @JsonProperty("response_format")
        ResponseFormat responseFormat,
        Thinking thinking,
        double temperature,
        @JsonProperty("max_tokens")
        int maxTokens
) {
    record DeepSeekMessage(String role, String content) {
    }

    record ResponseFormat(String type) {
    }

    record Thinking(String type) {
    }
}
