package com.javabanker.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.config.AnthropicCompatibilityConfig;
import com.javabanker.domain.AccountBalance;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Spring AI Anthropic client, configured by our application.yml, against a local fake
 * of the Anthropic Messages API. Checks the request contract (model, no sampling parameters, tools)
 * and the full tool-calling loop. No API key and no network needed.
 */
class AnthropicAssistantContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final List<JsonNode> requests = new ArrayList<>();
    private final List<Map<String, List<String>>> headers = new ArrayList<>();
    private final Deque<String> responses = new ArrayDeque<>();

    @BeforeEach
    void startFakeAnthropic() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            requests.add(JSON.readTree(exchange.getRequestBody()));
            headers.add(Map.copyOf(exchange.getRequestHeaders()));
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
    void callsClaudeWithToolsAndAnswersFromTheToolResult() {
        responses.add("""
                {"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"tool_use","id":"toolu_1","name":"getBalance","input":{}}],
                 "stop_reason":"tool_use","stop_sequence":null,"usage":{"input_tokens":50,"output_tokens":10}}
                """);
        responses.add("""
                {"id":"msg_2","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"text","text":"You have 70.00 EUR."}],
                 "stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":80,"output_tokens":8}}
                """);
        GetBalanceUseCase balances = accountId -> Optional.of(
                new AccountBalance(accountId, new BigDecimal("70.00"), "EUR", 2, "t2", Instant.parse("2026-01-01T10:00:00Z")));

        contextRunner().run(context -> {
            var assistant = new SpringAiBankAssistant(context.getBean(ChatClient.Builder.class), balances);

            assertThat(assistant.answer("acc-1", "How much money do I have?")).isEqualTo("You have 70.00 EUR.");
        });

        assertThat(requests).hasSize(2);
        JsonNode first = requests.get(0);
        assertThat(first.path("model").asText()).isEqualTo("claude-opus-5-5");
        assertThat(first.has("temperature")).as("current Claude models reject sampling parameters").isFalse();
        assertThat(first.path("max_tokens").asInt()).isEqualTo(4096);
        assertThat(first.path("system").toString()).contains("JavaBanker assistant");
        assertThat(first.findValuesAsText("name")).contains("getBalance", "rejectionReasons");
        if (first.has("tool_choice")) {
            assertThat(first.path("tool_choice").path("type").asText()).isEqualTo("auto");
        }
        assertThat(headers.get(0).get("X-api-key")).containsExactly("test-key");

        String secondRequest = requests.get(1).toString();
        assertThat(secondRequest).contains("tool_result", "toolu_1", "balance=70.00 EUR");
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer()) // loads src/main/resources/application.yml
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class, RestClientAutoConfiguration.class, WebClientAutoConfiguration.class,
                        SpringAiRetryAutoConfiguration.class, ToolCallingAutoConfiguration.class,
                        AnthropicChatAutoConfiguration.class, ChatClientAutoConfiguration.class))
                .withUserConfiguration(AnthropicCompatibilityConfig.class)
                .withPropertyValues(
                        "spring.ai.model.chat=anthropic",
                        "spring.ai.anthropic.api-key=test-key",
                        "spring.ai.anthropic.base-url=http://127.0.0.1:" + server.getAddress().getPort());
    }
}
