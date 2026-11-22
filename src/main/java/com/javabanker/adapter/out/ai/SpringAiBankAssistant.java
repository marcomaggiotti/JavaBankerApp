package com.javabanker.adapter.out.ai;

import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.application.port.out.BankAssistant;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Spring AI adapter. {@link ChatClient} hides the provider (Ollama, Claude, ...). When the model asks
 * for a tool, Spring AI runs the {@link BankingTools} method and sends the result back to the model
 * (the tool-calling loop) until the model returns a final answer.
 */
public class SpringAiBankAssistant implements BankAssistant {

    static final String SYSTEM_PROMPT = """
            You are the JavaBanker assistant. You help one customer with questions about their own bank account.
            Use the tools to look up facts; never invent balances or transactions.
            If the tools can't answer the question, say so briefly.
            Answer in the language of the question, in at most three sentences.
            """;

    private final ChatClient chatClient;
    private final GetBalanceUseCase getBalance;

    public SpringAiBankAssistant(ChatClient.Builder builder, GetBalanceUseCase getBalance) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
        this.getBalance = getBalance;
    }

    @Override
    public String answer(String accountId, String question) {
        try {
            String content = chatClient.prompt()
                    .user(question)
                    .tools(new BankingTools(accountId, getBalance))
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                throw new AssistantUnavailableException("The model returned an empty answer", null);
            }
            return content.strip();
        } catch (AssistantUnavailableException e) {
            throw e;
        } catch (RuntimeException e) { // connection refused, timeout, provider error ...
            throw new AssistantUnavailableException("The language model is not reachable: " + e.getMessage(), e);
        }
    }
}
