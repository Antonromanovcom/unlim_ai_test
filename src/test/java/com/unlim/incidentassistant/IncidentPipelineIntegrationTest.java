package com.unlim.incidentassistant;

import com.unlim.incidentassistant.llm.LlmClient;
import com.unlim.incidentassistant.llm.LlmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentPipelineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LlmClient llmClient;

    @BeforeEach
    void resetMock() {
        reset(llmClient);
    }

    @Test
    void analyzesIncidentAndAddsRelevantKnowledge() throws Exception {
        when(llmClient.generate(any())).thenReturn(validEnglishOutput());

        mockMvc.perform(analyzeRequest("""
                        {
                          "description": "Card payments fail because payment-service calls to PayGate time out."
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("External payment provider issue"))
                .andExpect(jsonPath("$.severity").value("high"))
                .andExpect(jsonPath("$.hypotheses[0].next_steps.length()").value(2));

        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).generate(requestCaptor.capture());
        String systemPrompt = requestCaptor.getValue().messages().getFirst().content();
        assertThat(systemPrompt)
                .contains("INC-101")
                .contains("PayGate")
                .doesNotContain("INC-103");
    }

    @Test
    void recoversAfterMalformedJson() throws Exception {
        when(llmClient.generate(any()))
                .thenReturn("{broken}")
                .thenReturn(validEnglishOutput());

        mockMvc.perform(analyzeRequest(englishRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("high"));

        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient, times(2)).generate(requestCaptor.capture());
        LlmRequest recoveryRequest = requestCaptor.getAllValues().get(1);
        assertThat(recoveryRequest.messages())
                .anySatisfy(message -> assertThat(message.content())
                        .contains("previous answer was rejected")
                        .contains("invalid JSON"));
    }

    @Test
    void recoversAfterEmptyOutput() throws Exception {
        when(llmClient.generate(any()))
                .thenReturn("")
                .thenReturn(validEnglishOutput());

        mockMvc.perform(analyzeRequest(englishRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("External payment provider issue"));

        verify(llmClient, times(2)).generate(any());
    }

    @Test
    void recoversAfterWrongLanguage() throws Exception {
        when(llmClient.generate(any()))
                .thenReturn(validRussianOutput())
                .thenReturn(validEnglishOutput());

        mockMvc.perform(analyzeRequest(englishRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("PayGate requests time out, so card payments fail."));

        verify(llmClient, times(2)).generate(any());
    }

    @Test
    void returnsBadGatewayAfterTwoInvalidResponses() throws Exception {
        when(llmClient.generate(any()))
                .thenReturn("{broken}")
                .thenReturn("""
                        {
                          "category": "",
                          "summary": "",
                          "severity": "high",
                          "hypotheses": []
                        }
                        """);

        mockMvc.perform(analyzeRequest(englishRequest()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message")
                        .value("LLM failed to produce a valid incident analysis after 2 attempts"));

        verify(llmClient, times(2)).generate(any());
    }

    @Test
    void returnsRussianAnalysisWhenRequested() throws Exception {
        when(llmClient.generate(any())).thenReturn(validRussianOutput());

        mockMvc.perform(analyzeRequest("""
                        {
                          "description": "Пользователи не могут оплатить картой из-за таймаутов PayGate.",
                          "response_language": "RUSSIAN"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Сбой внешнего платежного провайдера"))
                .andExpect(jsonPath("$.severity").value("high"));

        verify(llmClient).generate(any());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder analyzeRequest(
            String body
    ) {
        return post("/api/v1/incidents/analyze")
                .contentType(APPLICATION_JSON)
                .content(body);
    }

    private String englishRequest() {
        return """
                {
                  "description": "Card payments fail because PayGate requests time out.",
                  "response_language": "ENGLISH"
                }
                """;
    }

    private String validEnglishOutput() {
        return """
                {
                  "category": "External payment provider issue",
                  "summary": "PayGate requests time out, so card payments fail.",
                  "severity": "high",
                  "hypotheses": [
                    {
                      "title": "PayGate degradation",
                      "reasoning": "Timeouts are isolated to the provider calls.",
                      "next_steps": [
                        "Check the PayGate status page.",
                        "Compare PayGate latency and error metrics."
                      ]
                    }
                  ]
                }
                """;
    }

    private String validRussianOutput() {
        return """
                {
                  "category": "Сбой внешнего платежного провайдера",
                  "summary": "Запросы к PayGate завершаются таймаутом, поэтому платежи не проходят.",
                  "severity": "high",
                  "hypotheses": [
                    {
                      "title": "Сбой на стороне PayGate",
                      "reasoning": "Таймауты возникают только при обращении к платежному провайдеру.",
                      "next_steps": [
                        "Проверить страницу состояния PayGate.",
                        "Сравнить метрики задержек и ошибок провайдеров."
                      ]
                    }
                  ]
                }
                """;
    }
}
