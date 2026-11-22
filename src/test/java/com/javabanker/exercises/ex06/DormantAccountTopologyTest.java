package com.javabanker.exercises.ex06;

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

import static org.assertj.core.api.Assertions.assertThat;

/** The driver's wall clock starts at T0; advanceWallClockTime moves it and fires punctuations. */
@Tag("exercise")
class DormantAccountTopologyTest extends TopologyTestSupport {

    private TestInputTopic<String, Transaction> in;
    private TestOutputTopic<String, DormantAccountAlert> out;
    private Instant now = T0;

    @BeforeEach
    void setUp() {
        start(new DormantAccountTopology(Duration.ofSeconds(30), Duration.ofSeconds(5)));
        in = input(DormantAccountTopology.INPUT, Transaction.class);
        out = output(DormantAccountTopology.OUTPUT, DormantAccountAlert.class);
    }

    @Test
    void alertsOnlyInactiveAccountsOnce() {
        activity("acc-1");
        activity("acc-2");
        advance(20);
        activity("acc-2");
        advance(15); // now = T0+35s: acc-1 idle 35s, acc-2 idle 15s

        var alerts = out.readValuesToList();
        assertThat(alerts).extracting(DormantAccountAlert::accountId).containsExactly("acc-1");
        assertThat(alerts.get(0).lastActivity()).isEqualTo(T0);
        assertThat(alerts.get(0).detectedAt()).isAfterOrEqualTo(T0.plusSeconds(30)).isBeforeOrEqualTo(now);

        advance(20); // now = T0+55s: acc-2 idle 35s, acc-1 already alerted
        assertThat(out.readValuesToList()).extracting(DormantAccountAlert::accountId).containsExactly("acc-2");

        advance(60);
        assertThat(out.isEmpty()).as("no repeated alerts").isTrue();
    }

    @Test
    void newActivityResetsAndAccountCanBecomeDormantAgain() {
        activity("acc-1");
        advance(31);
        assertThat(out.readValuesToList()).hasSize(1);

        activity("acc-1");
        advance(10);
        assertThat(out.isEmpty()).isTrue();
        advance(25);

        var again = out.readValuesToList();
        assertThat(again).hasSize(1);
        assertThat(again.get(0).lastActivity()).isEqualTo(T0.plusSeconds(31));
    }

    @Test
    void activeAccountIsNeverAlerted() {
        for (int i = 0; i < 10; i++) {
            activity("acc-1");
            advance(10);
        }
        assertThat(out.isEmpty()).isTrue();
    }

    private void activity(String accountId) {
        in.pipeInput(accountId, tx(accountId, TransactionType.DEPOSIT, "1", now), now);
    }

    private void advance(long seconds) {
        now = now.plusSeconds(seconds);
        driver.advanceWallClockTime(Duration.ofSeconds(seconds));
    }
}
