package com.javabanker.adapter.in.web;

import com.javabanker.application.port.in.AskAssistantUseCase;
import com.javabanker.application.port.in.AskAssistantUseCase.AskAssistantCommand;
import com.javabanker.application.port.in.AskAssistantUseCase.AssistantAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/assistant {"accountId":"acc-1","question":"How much money do I have?"} */
@RestController
@RequestMapping("/api/assistant")
class AssistantController {

    private final AskAssistantUseCase askAssistant;

    AssistantController(AskAssistantUseCase askAssistant) {
        this.askAssistant = askAssistant;
    }

    @PostMapping
    AssistantAnswer ask(@Valid @RequestBody AssistantRequest request) {
        return askAssistant.ask(new AskAssistantCommand(request.accountId(), request.question()));
    }

    record AssistantRequest(@NotBlank String accountId, @NotBlank String question) {
    }
}
