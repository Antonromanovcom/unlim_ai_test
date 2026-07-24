package com.unlim.incidentassistant.llm;

import java.util.List;

public record LlmRequest(List<LlmMessage> messages) {

    public LlmRequest {
        messages = List.copyOf(messages);
    }
}
