package com.javabanker.streams.topology;

import com.javabanker.domain.FraudAlert;
import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;
import com.javabanker.streams.support.Topics;
import com.javabanker.support.TopologyTestSupport;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FraudDetectionTopologyTest extends TopologyTestSupport {

    private final Topics topics = Topics.defaults();
    private TestInputTopic<String, Transaction> transactions;
    private TestOutputTopic<String, FraudAlert> alerts;

    @BeforeEach
    void setUp() {
        start(new FraudDetectionTopology(topics, 3, Duration.ofMinutes(1), Duration.ofSeconds(10)));
        transactions = input(topics.transactions(), Transaction.class);
        alerts = output(topics.fraudAlerts(), FraudAlert.class);
    }

    @Test
    void alertsOncePerWindowWhenThresholdReached() {
        for (int i = 0; i < 4; i++) {
            withdrawAt("acc-1", T0.plusSeconds(i * 10L));
        }
        assertThat(alerts.isEmpty()).as("suppressed until the window closes").isTrue();

        advanceStreamTimePastWindow();

        FraudAlert alert = alerts.readValue();
        assertThat(alert.accountId()).isEqualTo("acc-1");
        assertThat(alert.observedCount()).isEqualTo(4);
        assertThat(alert.windowStart()).isEqualTo(T0);
        assertThat(alerts.isEmpty()).isTrue();
    }

    @Test
    void noAlertBelowThresholdOrForDeposits() {
        withdrawAt("acc-1", T0);
        withdrawAt("acc-1", T0.plusSeconds(5));
        for (int i = 0; i < 5; i++) {
            transactions.pipeInput("acc-1", tx("acc-1", TransactionType.DEPOSIT, "1", T0), T0);
        }
        advanceStreamTimePastWindow();

        assertThat(alerts.isEmpty()).isTrue();
    }

    @Test
    void withdrawalsInDifferentWindowsAreNotCombined() {
        withdrawAt("acc-1", T0.plusSeconds(50));
        withdrawAt("acc-1", T0.plusSeconds(55));
        withdrawAt("acc-1", T0.plusSeconds(65)); // next window
        advanceStreamTimePastWindow();

        assertThat(alerts.isEmpty()).isTrue();
    }

    private void withdrawAt(String accountId, java.time.Instant at) {
        transactions.pipeInput(accountId, tx(accountId, TransactionType.WITHDRAWAL, "10", at), at);
    }

    /** Suppression is driven by STREAM time: only a newer record closes the window. */
    private void advanceStreamTimePastWindow() {
        withdrawAt("clock-tick", T0.plus(Duration.ofMinutes(5)));
    }
}
