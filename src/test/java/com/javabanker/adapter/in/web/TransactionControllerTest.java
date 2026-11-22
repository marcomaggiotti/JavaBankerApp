package com.javabanker.adapter.in.web;

import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.application.port.in.SubmitTransactionUseCase;
import com.javabanker.domain.AccountBalance;
import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web slice test: only MVC infrastructure is started, use cases are mocked. */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SubmitTransactionUseCase submitTransaction;

    @MockitoBean
    GetBalanceUseCase getBalance;

    @Test
    void acceptsValidTransaction() throws Exception {
        given(submitTransaction.submit(any())).willReturn(new Transaction(
                "tx-1", "acc-1", TransactionType.DEPOSIT, BigDecimal.TEN, "EUR", Instant.parse("2026-01-01T00:00:00Z")));

        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"accountId":"acc-1","type":"DEPOSIT","amount":10.00,"currency":"EUR"}
                        """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"accountId":"","type":"DEPOSIT","amount":-1,"currency":"euro"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBalanceOr404() throws Exception {
        given(getBalance.balanceOf("acc-1")).willReturn(Optional.of(
                new AccountBalance("acc-1", new BigDecimal("42.00"), "EUR", 3, "tx-3", Instant.parse("2026-01-01T00:00:00Z"))));
        given(getBalance.balanceOf("missing")).willReturn(Optional.empty());

        mvc.perform(get("/api/accounts/acc-1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(42.00));
        mvc.perform(get("/api/accounts/missing/balance")).andExpect(status().isNotFound());
    }
}
