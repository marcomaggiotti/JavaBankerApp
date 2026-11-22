package com.javabanker.application.service;

import com.javabanker.application.port.in.AskAssistantUseCase;
import com.javabanker.application.port.out.BankAssistant;

/** Validates the request (cheap checks before an expensive LLM call), then delegates to the model port. */
public class AskAssistantService implements AskAssistantUseCase {

    private final BankAssistant assistant;
    private final int maxQuestionLength;

    public AskAssistantService(BankAssistant assistant, int maxQuestionLength) {
        this.assistant = assistant;
        this.maxQuestionLength = maxQuestionLength;
    }

    @Override
    public AssistantAnswer ask(AskAssistantCommand command) {
        if (command.accountId() == null || command.accountId().isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        String question = command.question() == null ? "" : command.question().strip();
        if (question.isEmpty()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (question.length() > maxQuestionLength) {
            throw new IllegalArgumentException("question must be at most " + maxQuestionLength + " characters");
        }
        return new AssistantAnswer(command.accountId(), assistant.answer(command.accountId(), question));
    }
}
