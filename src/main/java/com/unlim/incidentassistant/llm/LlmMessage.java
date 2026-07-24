package com.unlim.incidentassistant.llm;

public record LlmMessage(
        Role role,
        String content
) {
    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT
    }
}
