package com.unlim.incidentassistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.llm.deepseek.api-key=")
@AutoConfigureMockMvc
class MissingApiKeyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsServiceUnavailableWithoutCallingTheNetwork() throws Exception {
        mockMvc.perform(post("/api/v1/incidents/analyze")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Card payments fail because PayGate requests time out."
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("DeepSeek API key is not configured"));
    }
}
