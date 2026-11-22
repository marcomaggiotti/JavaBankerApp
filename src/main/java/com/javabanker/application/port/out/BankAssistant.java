package com.javabanker.application.port.out;

/**
 * Outbound port to a language model. The application core doesn't know whether a local Ollama model,
 * Claude or a test fake answers: that's decided by the adapter (Spring AI in production).
 */
public interface BankAssistant {

    /**
     * Answers {@code question} for {@code accountId}. Implementations may only look up data of that account.
     *
     * @throws AssistantUnavailableException if no model is configured or it can't be reached
     */
    String answer(String accountId, String question);

    class AssistantUnavailableException extends RuntimeException {
        public AssistantUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
