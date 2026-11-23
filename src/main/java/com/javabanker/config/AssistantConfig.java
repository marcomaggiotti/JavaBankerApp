package com.javabanker.config;

import com.javabanker.adapter.out.ai.SpringAiBankAssistant;
import com.javabanker.application.port.in.AskAssistantUseCase;
import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.application.port.out.BankAssistant;
import com.javabanker.application.service.AskAssistantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the assistant. Spring AI creates a {@link ChatClient.Builder} only when a chat model is enabled
 * (spring.ai.model.chat=ollama|anthropic); with "none" the assistant answers 503 instead of failing startup.
 */
@Configuration(proxyBeanMethods = false)
public class AssistantConfig {

    @Bean
    BankAssistant bankAssistant(ObjectProvider<ChatClient.Builder> chatClientBuilder, GetBalanceUseCase getBalance) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            // BankAssistant has a single abstract method, answer(accountId, question), so a lambda can
            // implement it directly: no class needed for this tiny "always unavailable" fallback.
            // A lambda body may throw instead of returning: the method then always fails (here: HTTP 503).
            return (accountId, question) -> {
                throw new BankAssistant.AssistantUnavailableException(
                        "No chat model configured (spring.ai.model.chat=none)", null);
            };
        }
        return new SpringAiBankAssistant(builder, getBalance);
    }

    @Bean
    AskAssistantUseCase askAssistantUseCase(BankAssistant assistant, BankerProperties props) {
        return new AskAssistantService(assistant, props.assistant().maxQuestionLength());
    }
}
