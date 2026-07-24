package com.unlim.incidentassistant.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.unlim.incidentassistant.api.model.ResponseLanguage;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentResponseParserTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();
    private final IncidentResponseParser parser = new IncidentResponseParser(
            objectMapper,
            Validation.buildDefaultValidatorFactory().getValidator()
    );

    @Test
    void parsesValidStructuredOutput() {
        var analysis = parser.parseAndValidate(validEnglishJson(), ResponseLanguage.ENGLISH);

        assertThat(analysis.category()).isEqualTo("External payment provider issue");
        assertThat(analysis.hypotheses()).hasSize(1);
        assertThat(analysis.hypotheses().getFirst().nextSteps()).hasSize(2);
    }

    @Test
    void rejectsWrongLanguage() {
        String russianOutput = validEnglishJson()
                .replace("External payment provider issue", "Ошибка платежного провайдера")
                .replace("PayGate is timing out.", "PayGate не отвечает вовремя.")
                .replace("Provider degradation", "Сбой у провайдера")
                .replace("Timeouts affect payments.", "Таймауты мешают платежам.")
                .replace("Check provider status.", "Проверить статус провайдера.")
                .replace("Compare latency metrics.", "Сравнить метрики задержки.");

        assertThatThrownBy(() -> parser.parseAndValidate(russianOutput, ResponseLanguage.ENGLISH))
                .isInstanceOf(ModelOutputException.class)
                .hasMessageContaining("language");
    }

    @Test
    void rejectsTooFewDiagnosticSteps() {
        String invalidOutput = validEnglishJson()
                .replace("\"Check provider status.\", \"Compare latency metrics.\"",
                        "\"Check provider status.\"");

        assertThatThrownBy(() -> parser.parseAndValidate(invalidOutput, ResponseLanguage.ENGLISH))
                .isInstanceOf(ModelOutputException.class)
                .hasMessageContaining("nextSteps");
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> parser.parseAndValidate("{not-json}", ResponseLanguage.ENGLISH))
                .isInstanceOf(ModelOutputException.class)
                .hasMessageContaining("invalid JSON");
    }

    private String validEnglishJson() {
        return """
                {
                  "category": "External payment provider issue",
                  "summary": "PayGate is timing out.",
                  "severity": "high",
                  "hypotheses": [
                    {
                      "title": "Provider degradation",
                      "reasoning": "Timeouts affect payments.",
                      "next_steps": ["Check provider status.", "Compare latency metrics."]
                    }
                  ]
                }
                """;
    }
}
