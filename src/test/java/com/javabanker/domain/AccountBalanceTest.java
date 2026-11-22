package com.javabanker.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountBalanceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void depositIncreasesBalance() {
        TransactionOutcome outcome = AccountBalance.open("acc-1", "EUR").apply(tx("t1", TransactionType.DEPOSIT, "100.00"));

        assertThat(outcome.isAccepted()).isTrue();
        assertThat(outcome.balance().balance()).isEqualByComparingTo("100.00");
        assertThat(outcome.balance().transactionCount()).isEqualTo(1);
        assertThat(outcome.balance().lastTransactionId()).isEqualTo("t1");
    }

    @Test
    void withdrawalThatWouldOverdrawIsRejectedAndBalanceUnchanged() {
        AccountBalance funded = AccountBalance.open("acc-1", "EUR").apply(tx("t1", TransactionType.DEPOSIT, "50")).balance();

        TransactionOutcome outcome = funded.apply(tx("t2", TransactionType.WITHDRAWAL, "50.01"));

        assertThat(outcome.isAccepted()).isFalse();
        assertThat(outcome.reason()).isEqualTo(RejectionReason.INSUFFICIENT_FUNDS);
        assertThat(outcome.balance()).isEqualTo(funded);
    }

    @Test
    void withdrawingTheExactBalanceIsAllowed() {
        AccountBalance funded = AccountBalance.open("acc-1", "EUR").apply(tx("t1", TransactionType.DEPOSIT, "50")).balance();

        assertThat(funded.apply(tx("t2", TransactionType.WITHDRAWAL, "50")).balance().balance()).isZero();
    }

    @Test
    void currencyMismatchIsRejected() {
        Transaction usd = new Transaction("t1", "acc-1", TransactionType.DEPOSIT, BigDecimal.TEN, "USD", NOW);

        assertThat(AccountBalance.open("acc-1", "EUR").apply(usd).reason()).isEqualTo(RejectionReason.CURRENCY_MISMATCH);
    }

    @Test
    void transactionRejectsInvalidValues() {
        assertThatThrownBy(() -> tx("t1", TransactionType.DEPOSIT, "0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tx("t1", TransactionType.DEPOSIT, "-5")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Transaction("t1", "acc-1", TransactionType.DEPOSIT, BigDecimal.ONE, "euro", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Transaction tx(String id, TransactionType type, String amount) {
        return new Transaction(id, "acc-1", type, new BigDecimal(amount), "EUR", NOW);
    }
}
