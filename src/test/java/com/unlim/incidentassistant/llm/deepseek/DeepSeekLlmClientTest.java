package com.unlim.incidentassistant.llm.deepseek;

import com.unlim.incidentassistant.llm.LlmMessage;
import com.unlim.incidentassistant.llm.LlmRequest;
import com.unlim.incidentassistant.llm.LlmUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static com.unlim.incidentassistant.llm.LlmMessage.Role.SYSTEM;
import static com.unlim.incidentassistant.llm.LlmMessage.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeepSeekLlmClientTest {

    private MockRestServiceServer server;
    private DeepSeekLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://deepseek.test");
        server = MockRestServiceServer.bindTo(builder).build();
        DeepSeekProperties properties = new DeepSeekProperties(
                "test-key",
                "https://deepseek.test",
                "deepseek-v4-flash",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                1500
        );
        client = new DeepSeekLlmClient(builder.build(), properties);
    }

    @Test
    void sendsJsonModeRequestAndReturnsMessageContent() {
        server.expect(once(), requestTo("https://deepseek.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("deepseek-v4-flash"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.thinking.type").value("disabled"))
                .andExpect(jsonPath("$.max_tokens").value(1500))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"category\\":\\"Payment issue\\"}"
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String result = client.generate(request());

        assertThat(result).isEqualTo("{\"category\":\"Payment issue\"}");
        server.verify();
    }

    @Test
    void mapsRateLimitWithoutExposingProviderBody() {
        server.expect(requestTo("https://deepseek.test/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"sensitive provider details\"}"));

        assertThatThrownBy(() -> client.generate(request()))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("DeepSeek rate limit exceeded")
                .hasMessageNotContaining("sensitive");
    }

    @Test
    void refusesToCallProviderWithoutApiKey() {
        DeepSeekProperties properties = new DeepSeekProperties(
                "",
                "https://deepseek.test",
                "deepseek-v4-flash",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                1500
        );
        DeepSeekLlmClient clientWithoutKey = new DeepSeekLlmClient(RestClient.create(), properties);

        assertThatThrownBy(() -> clientWithoutKey.generate(request()))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessage("DeepSeek API key is not configured");
    }

    private LlmRequest request() {
        return new LlmRequest(List.of(
                new LlmMessage(SYSTEM, "Return JSON"),
                new LlmMessage(USER, "Analyze incident")
        ));
    }
}
