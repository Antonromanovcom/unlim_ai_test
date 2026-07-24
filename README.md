# AI Incident Assistant

A small Spring Boot service that helps on-call engineers triage production incidents. It accepts
a free-form incident description, enriches it with known system context and similar historical
incidents, asks an LLM for analysis, validates the result, and returns machine-readable JSON.

## Requirements

- JDK 21
- A DeepSeek API key for real analysis

Maven does not need to be installed. The repository includes Maven Wrapper 3.9.16.

## Quick start

1. Export the API key:

   ```bash
   export DEEPSEEK_API_KEY="your-api-key"
   ```

2. Start the application:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Send an incident:

   ```bash
   curl --request POST http://localhost:8080/api/v1/incidents/analyze \
     --header 'Content-Type: application/json' \
     --data '{
       "description": "Customers cannot pay by card. payment-service logs show PayGate timeouts.",
       "response_language": "ENGLISH"
     }'
   ```

`response_language` is optional and defaults to `ENGLISH`. Supported values are `ENGLISH` and
`RUSSIAN`.

Example response:

```json
{
  "category": "External payment provider issue",
  "summary": "PayGate requests time out, causing card payment failures.",
  "severity": "high",
  "hypotheses": [
    {
      "title": "PayGate degradation",
      "reasoning": "Timeouts are isolated to calls to the external provider.",
      "next_steps": [
        "Check the PayGate status page and provider notifications.",
        "Compare PayGate latency and error metrics with other providers."
      ]
    }
  ]
}
```

## Agent architecture

The LLM is one controlled stage of a multi-step pipeline:

1. `IncidentInputParser` normalizes the input and extracts terms used for retrieval.
2. `IncidentContextRetriever` selects up to two relevant historical incidents and adds the fixed
   payment-platform description.
3. `PromptFactory` creates an explicit prompt containing the evidence, output language, and JSON
   contract.
4. `DeepSeekLlmClient` calls the provider in JSON mode.
5. `IncidentResponseParser` deserializes the response into typed Java records and validates all
   structural constraints and the expected language.
6. If validation fails, the agent sends one recovery request containing the invalid response and
   the exact validation error. A second invalid response becomes a controlled `502 Bad Gateway`.

The system description and historical examples live under `src/main/resources/knowledge`, so the
domain context can be reviewed and changed independently from the orchestration code.

## Output contract

- `category`: non-empty classification chosen from the evidence; it is not restricted to four
  hard-coded categories.
- `summary`: concise description of what is happening and who is affected.
- `severity`: `low`, `medium`, or `high`.
- `hypotheses`: one to three possible root causes.
- Each hypothesis contains a title, reasoning, and two or three concrete diagnostic steps.

Unknown request fields, malformed JSON, and descriptions shorter than 10 or longer than 10,000
characters are rejected with `400 Bad Request`.

## Configuration

| Environment variable | Required | Default | Description |
| --- | --- | --- | --- |
| `DEEPSEEK_API_KEY` | yes | - | Provider API key |
| `DEEPSEEK_BASE_URL` | no | `https://api.deepseek.com` | Provider base URL |
| `DEEPSEEK_MODEL` | no | `deepseek-v4-flash` | Model name |
| `DEEPSEEK_CONNECT_TIMEOUT` | no | `3s` | Connection timeout |
| `DEEPSEEK_READ_TIMEOUT` | no | `30s` | Response timeout |
| `DEEPSEEK_MAX_TOKENS` | no | `1500` | Maximum response tokens |

The client uses `response_format: {"type":"json_object"}`, a low temperature, and disabled
thinking mode to favor short and stable structured responses. API keys and local `.env` files are
ignored by Git.

## Tests

Run the complete test suite:

```bash
./mvnw test
```

Build the executable JAR:

```bash
./mvnw clean package
java -jar target/incident-assistant-0.0.1-SNAPSHOT.jar
```

The standard test suite never calls DeepSeek. The `LlmClient` boundary is mocked, and the HTTP
client is verified with Spring's local mock server. Tests cover:

- card-payment failures caused by PayGate timeouts: expect an external provider category and high
  severity;
- slow payment creation with reporting queries and high database CPU: expect database degradation;
- missing confirmation e-mails with successful balance updates: expect notification degradation;
- `401` responses with invalid token signatures: expect authentication errors;
- malformed JSON, empty output, an unexpected language, and an invalid schema: expect one recovery
  attempt;
- two invalid model responses: expect a controlled `502`;
- missing API key: expect a controlled `503` without a network call.

## Error handling

- `400 Bad Request`: invalid client input.
- `502 Bad Gateway`: the model returned invalid output twice.
- `503 Service Unavailable`: API key is missing, credentials are rejected, rate limits are reached,
  or the provider cannot be contacted.

Provider response bodies and secrets are not returned to API clients.

## Trade-offs

To keep the take-home solution compact, context retrieval uses deterministic keyword scoring
instead of embeddings or a vector database. Language validation uses script predominance rather
than a language-detection model. The service is stateless and performs a maximum of two LLM calls
per request.

With more time, I would add observability for provider latency, token usage and recovery rate;
resilience policies such as circuit breaking; a versioned incident knowledge store; semantic
retrieval; authentication and request limits; and evaluation against a larger labelled incident
dataset.

## API reference

- Endpoint: `POST /api/v1/incidents/analyze`
- Content type: `application/json`
- DeepSeek API: <https://api-docs.deepseek.com/api/create-chat-completion>
