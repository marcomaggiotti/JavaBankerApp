package com.javabanker.application.port.in;

/** Inbound port: answer a customer's natural-language question about their own account. */
public interface AskAssistantUseCase {

    AssistantAnswer ask(AskAssistantCommand command);

    record AskAssistantCommand(String accountId, String question) {
    }

    record AssistantAnswer(String accountId, String answer) {
    }
}
