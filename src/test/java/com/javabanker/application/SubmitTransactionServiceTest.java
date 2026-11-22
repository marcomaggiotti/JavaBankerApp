package com.javabanker.application;

import com.javabanker.application.port.in.SubmitTransactionUseCase.SubmitTransactionCommand;
import com.javabanker.application.service.SubmitTransactionService;
import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit test: a hand-written fake for the outbound port, fixed clock and id generator. */
class SubmitTransactionServiceTest {

    private final List<Transaction> published = new ArrayList<>();
    private final Instant now = Instant.parse("2026-03-01T12:00:00Z");
    private final SubmitTransactionService service = new SubmitTransactionService(
            published::add, Clock.fixed(now, ZoneOffset.UTC), () -> "tx-42");

    @Test
    void createsTransactionAndPublishesIt() {
        Transaction tx = service.submit(new SubmitTransactionCommand("acc-1", TransactionType.DEPOSIT, new BigDecimal("12.50"), "EUR"));

        assertThat(tx.transactionId()).isEqualTo("tx-42");
        assertThat(tx.timestamp()).isEqualTo(now);
        assertThat(published).containsExactly(tx);
    }
}
