package com.unlim.incidentassistant.llm.deepseek;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.llm.deepseek")
public record DeepSeekProperties(
        String apiKey,
        String baseUrl,
        String model,
        Duration connectTimeout,
        Duration readTimeout,
        int maxTokens
) {
}
