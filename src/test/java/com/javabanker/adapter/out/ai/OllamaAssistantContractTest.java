package com.javabanker.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.domain.AccountBalance;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same as AnthropicAssistantContractTest for the default, token-free provider: the real Spring AI
 * Ollama client against a local fake of Ollama's /api/chat endpoint.
 */
class OllamaAssistantContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final List<JsonNode> requests = new ArrayList<>();
    private final Deque<String> responses = new ArrayDeque<>();

    @BeforeEach
    void startFakeOllama() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            requests.add(JSON.readTree(exchange.getRequestBody()));
            byte[] body = responses.removeFirst().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void callsOllamaWithToolsAndAnswersFromTheToolResult() {
        responses.add("""
                {"model":"qwen2.5:3b","created_at":"2026-01-01T10:00:00Z",
                 "message":{"role":"assistant","content":"","tool_calls":[{"function":{"name":"getBalance","arguments":{}}}]},
                 "done":true,"done_reason":"stop","prompt_eval_count":50,"eval_count":10}
                """);
        responses.add("""
                {"model":"qwen2.5:3b","created_at":"2026-01-01T10:00:01Z",
                 "message":{"role":"assistant","content":"You have 70.00 EUR."},
                 "done":true,"done_reason":"stop","prompt_eval_count":80,"eval_count":8}
                """);
        GetBalanceUseCase balances = accountId -> Optional.of(
                new AccountBalance(accountId, new BigDecimal("70.00"), "EUR", 2, "t2", Instant.parse("2026-01-01T10:00:00Z")));

        contextRunner().run(context -> {
            var assistant = new SpringAiBankAssistant(context.getBean(ChatClient.Builder.class), balances);

            assertThat(assistant.answer("acc-1", "How much money do I have?")).isEqualTo("You have 70.00 EUR.");
        });

        assertThat(requests).hasSize(2);
        JsonNode first = requests.get(0);
        assertThat(first.path("model").asText()).isEqualTo("qwen2.5:3b");
        assertThat(first.path("stream").asBoolean(true)).isFalse();
        assertThat(first.path("messages").toString()).contains("JavaBanker assistant");
        assertThat(first.findValuesAsText("name")).contains("getBalance", "rejectionReasons");
        assertThat(requests.get(1).toString()).contains("\"role\":\"tool\"", "balance=70.00 EUR");
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer()) // loads src/main/resources/application.yml
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class, RestClientAutoConfiguration.class, WebClientAutoConfiguration.class,
                        SpringAiRetryAutoConfiguration.class, ToolCallingAutoConfiguration.class,
                        OllamaApiAutoConfiguration.class, OllamaChatAutoConfiguration.class, ChatClientAutoConfiguration.class))
                .withPropertyValues(
                        "spring.ai.model.chat=ollama",
                        "spring.ai.ollama.base-url=http://127.0.0.1:" + server.getAddress().getPort());
    }
}
