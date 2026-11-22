package com.javabanker.application;

import com.javabanker.application.port.in.AskAssistantUseCase.AskAssistantCommand;
import com.javabanker.application.service.AskAssistantService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AskAssistantServiceTest {

    private final List<String> calls = new ArrayList<>();
    private final AskAssistantService service = new AskAssistantService((accountId, question) -> {
        calls.add(accountId + ":" + question);
        return "42 EUR";
    }, 20);

    @Test
    void delegatesTrimmedQuestionToTheModel() {
        var answer = service.ask(new AskAssistantCommand("acc-1", "  How much?  "));

        assertThat(answer.answer()).isEqualTo("42 EUR");
        assertThat(calls).containsExactly("acc-1:How much?");
    }

    @Test
    void rejectsInvalidQuestionsBeforeCallingTheModel() {
        assertThatThrownBy(() -> service.ask(new AskAssistantCommand("acc-1", " "))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.ask(new AskAssistantCommand("", "hi"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.ask(new AskAssistantCommand("acc-1", "x".repeat(21)))).isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).isEmpty();
    }
}
