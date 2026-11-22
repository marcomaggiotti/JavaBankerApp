package com.javabanker.exercises.ex05;

import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;
import com.javabanker.support.TopologyTestSupport;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("exercise")
class DailyStatementTopologyTest extends TopologyTestSupport {

    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_2 = LocalDate.of(2026, 1, 2);

    private TestInputTopic<String, Transaction> in;
    private TestOutputTopic<String, DailyStatement> out;

    @BeforeEach
    void setUp() {
        start(new DailyStatementTopology(Duration.ofHours(1)));
        in = input(DailyStatementTopology.INPUT, Transaction.class);
        out = output(DailyStatementTopology.OUTPUT, DailyStatement.class);
    }

    @Test
    void emitsOneFinalStatementWhenTheDayCloses() {
        send("acc-1", TransactionType.DEPOSIT, "100", at("2026-01-01T09:00:00Z"));
        send("acc-1", TransactionType.DEPOSIT, "50", at("2026-01-01T12:00:00Z"));
        send("acc-1", TransactionType.WITHDRAWAL, "30", at("2026-01-01T18:00:00Z"));
        assertThat(out.isEmpty()).as("day still open").isTrue();

        tick("2026-01-02T00:30:00Z");
        assertThat(out.isEmpty()).as("still within grace").isTrue();

        tick("2026-01-02T02:00:00Z");
        List<DailyStatement> statements = statementsOf("acc-1");
        assertThat(statements).hasSize(1);
        DailyStatement s = statements.get(0);
        assertThat(s.day()).isEqualTo(JAN_1);
        assertThat(s.totalDeposits()).isEqualByComparingTo("150");
        assertThat(s.totalWithdrawals()).isEqualByComparingTo("30");
        assertThat(s.net()).isEqualByComparingTo("120");
        assertThat(s.transactionCount()).isEqualTo(3);
    }

    @Test
    void lateEventWithinGraceIsIncluded() {
        send("acc-1", TransactionType.DEPOSIT, "100", at("2026-01-01T10:00:00Z"));
        tick("2026-01-02T00:30:00Z");
        send("acc-1", TransactionType.DEPOSIT, "5", at("2026-01-01T23:59:00Z")); // 31 min late, grace = 1h
        tick("2026-01-02T02:00:00Z");

        DailyStatement s = statementsOf("acc-1").get(0);
        assertThat(s.totalDeposits()).isEqualByComparingTo("105");
        assertThat(s.transactionCount()).isEqualTo(2);
    }

    @Test
    void eventAfterGraceIsDroppedAndNoSecondStatement() {
        send("acc-1", TransactionType.DEPOSIT, "100", at("2026-01-01T10:00:00Z"));
        tick("2026-01-02T02:00:00Z");
        send("acc-1", TransactionType.DEPOSIT, "999", at("2026-01-01T12:00:00Z")); // too late
        tick("2026-01-03T02:00:00Z");

        List<DailyStatement> statements = statementsOf("acc-1");
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0).totalDeposits()).isEqualByComparingTo("100");
    }

    @Test
    void separateStatementsPerDayAndAccount() {
        send("acc-1", TransactionType.DEPOSIT, "10", at("2026-01-01T10:00:00Z"));
        send("acc-2", TransactionType.DEPOSIT, "20", at("2026-01-01T11:00:00Z"));
        send("acc-1", TransactionType.DEPOSIT, "30", at("2026-01-02T10:00:00Z"));
        tick("2026-01-03T02:00:00Z");

        List<DailyStatement> all = out.readValuesToList();
        assertThat(all).filteredOn(s -> s.accountId().equals("acc-1"))
                .extracting(DailyStatement::day).containsExactlyInAnyOrder(JAN_1, JAN_2);
        assertThat(all).filteredOn(s -> s.accountId().equals("acc-2"))
                .singleElement().extracting(DailyStatement::totalDeposits)
                .satisfies(v -> assertThat(v).isEqualByComparingTo("20"));
    }

    private List<DailyStatement> statementsOf(String accountId) {
        return out.readValuesToList().stream().filter(s -> s.accountId().equals(accountId)).toList();
    }

    private void send(String accountId, TransactionType type, String amount, Instant at) {
        in.pipeInput(accountId, tx(accountId, type, amount, at), at);
    }

    /** Advances stream time with a record on an unrelated account. */
    private void tick(String instant) {
        send("clock-tick", TransactionType.DEPOSIT, "1", at(instant));
    }

    private static Instant at(String instant) {
        return Instant.parse(instant);
    }
}
