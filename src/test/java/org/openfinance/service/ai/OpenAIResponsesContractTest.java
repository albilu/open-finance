package org.openfinance.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class OpenAIResponsesContractTest {
    private HttpServer server;
    private OpenAIProvider provider;
    private final AtomicReference<JsonNode> request = new AtomicReference<>();
    private String contentType = "application/json";
    private String response;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/responses",
                exchange -> {
                    request.set(new ObjectMapper().readTree(exchange.getRequestBody()));
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.start();
        provider =
                new OpenAIProvider(
                        "audit-dummy",
                        "gpt-4o-mini",
                        0.7,
                        2048,
                        5,
                        "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsSearchToResponsesAndReadsMessageAfterToolOutput() {
        response =
                "{\"status\":\"completed\",\"output\":[{\"type\":\"web_search_call\"},{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"Answer\"}]}]}";
        assertThat(
                        provider.sendPrompt("Quoted \"prompt\"\nNext line", "Context")
                                .block(Duration.ofSeconds(10)))
                .isEqualTo("Answer");
        assertThat(request.get().path("input").asText()).isEqualTo("Quoted \"prompt\"\nNext line");
        assertThat(request.get().path("tools").get(0).path("type").asText())
                .isEqualTo("web_search");
        assertThat(request.get().path("max_output_tokens").asInt()).isEqualTo(2048);
        assertThat(request.get().has("messages")).isFalse();
        assertThat(request.get().path("store").asBoolean()).isFalse();
    }

    @Test
    void parsesTypedResponseEventsWithoutRepeatingCompletedText() {
        contentType = "text/event-stream";
        response =
                "event: response.output_text.delta\ndata: {\"type\":\"response.output_text.delta\",\"delta\":\"Hello\"}\n\n"
                        + "event: response.output_text.delta\ndata: {\"type\":\"response.output_text.delta\",\"delta\":\" world\"}\n\n"
                        + "event: response.completed\ndata: {\"type\":\"response.completed\"}\n\n";
        StepVerifier.create(provider.streamResponse("Prompt", "Context"))
                .expectNext("Hello", " world")
                .verifyComplete();
        assertThat(request.get().path("stream").asBoolean()).isTrue();
    }

    @Test
    void propagatesAnExplicitProviderFailure() {
        response = "{\"status\":\"failed\",\"error\":{\"message\":\"Unavailable\"}}";
        StepVerifier.create(provider.sendPrompt("Prompt", "Context"))
                .expectError(AIProviderException.class)
                .verify();
    }
}
