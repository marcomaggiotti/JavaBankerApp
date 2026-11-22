package com.javabanker.adapter.out.ai;

import com.javabanker.application.port.out.BalanceRepository.BalanceUnavailableException;
import com.javabanker.domain.AccountBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BankingToolsTest {

    @Test
    void getBalanceOnlyReadsTheBoundAccount() {
        var tools = new BankingTools("acc-1", accountId -> accountId.equals("acc-1")
                ? Optional.of(new AccountBalance("acc-1", new BigDecimal("70.00"), "EUR", 2, "t2", Instant.parse("2026-01-01T10:00:00Z")))
                : Optional.empty());

        assertThat(tools.getBalance()).isEqualTo("balance=70.00 EUR, transactions=2, updatedAt=2026-01-01T10:00:00Z");
    }

    @Test
    void reportsMissingAndUnavailableBalances() {
        assertThat(new BankingTools("acc-1", id -> Optional.empty()).getBalance()).contains("No balance yet");
        assertThat(new BankingTools("acc-1", id -> {
            throw new BalanceUnavailableException("rebalancing", null);
        }).getBalance()).contains("temporarily unavailable");
    }

    @Test
    void explainsEveryRejectionReason() {
        assertThat(new BankingTools("acc-1", id -> Optional.empty()).rejectionReasons())
                .contains("INSUFFICIENT_FUNDS", "CURRENCY_MISMATCH", "WRONG_ACCOUNT");
    }
}
