package com.javabanker.adapter.in.web;

import com.javabanker.application.port.in.AskAssistantUseCase;
import com.javabanker.application.port.in.AskAssistantUseCase.AssistantAnswer;
import com.javabanker.application.port.out.BankAssistant.AssistantUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
class AssistantControllerTest {

    private static final String BODY = """
            {"accountId":"acc-1","question":"How much money do I have?"}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AskAssistantUseCase askAssistant;

    @Test
    void returnsTheAnswer() throws Exception {
        given(askAssistant.ask(any())).willReturn(new AssistantAnswer("acc-1", "You have 70.00 EUR."));

        mvc.perform(post("/api/assistant").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("You have 70.00 EUR."));
    }

    @Test
    void modelDownIsA503() throws Exception {
        given(askAssistant.ask(any())).willThrow(new AssistantUnavailableException("connection refused", null));

        mvc.perform(post("/api/assistant").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable());
    }
}
