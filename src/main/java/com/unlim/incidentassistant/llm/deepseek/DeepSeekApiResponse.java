package com.unlim.incidentassistant.llm.deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record DeepSeekApiResponse(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(
            Message message,
            @JsonProperty("finish_reason")
            String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {
    }
}
