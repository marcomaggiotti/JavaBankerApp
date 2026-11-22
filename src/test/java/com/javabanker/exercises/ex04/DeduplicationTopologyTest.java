package com.javabanker.exercises.ex04;

import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;
import com.javabanker.support.TopologyTestSupport;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("exercise")
class DeduplicationTopologyTest extends TopologyTestSupport {

    private TestInputTopic<String, Transaction> in;
    private TestOutputTopic<String, Transaction> out;

    @BeforeEach
    void setUp() {
        start(new DeduplicationTopology(Duration.ofMinutes(10)));
        in = input(DeduplicationTopology.INPUT, Transaction.class);
        out = output(DeduplicationTopology.OUTPUT, Transaction.class);
    }

    @Test
    void forwardsFirstOccurrenceAndDropsRetries() {
        send("tx-A", T0);
        send("tx-A", T0.plus(Duration.ofMinutes(5)));
        send("tx-B", T0.plus(Duration.ofMinutes(6)));
        send("tx-A", T0.plus(Duration.ofMinutes(9)));

        var forwarded = out.readKeyValuesToList();
        assertThat(forwarded).extracting(kv -> kv.value.transactionId()).containsExactly("tx-A", "tx-B");
        assertThat(forwarded).extracting(kv -> kv.key).containsOnly("acc-1");
    }

    @Test
    void sameIdAfterWindowIsForwardedAgain() {
        send("tx-A", T0);
        send("tx-A", T0.plus(Duration.ofMinutes(11)));

        assertThat(out.readValuesToList()).extracting(Transaction::transactionId).containsExactly("tx-A", "tx-A");
    }

    @Test
    void valueIsForwardedUnchanged() {
        Transaction tx = send("tx-A", T0);

        assertThat(out.readValue()).isEqualTo(tx);
    }

    @Test
    void nullValuesAreDropped() {
        in.pipeInput("acc-1", (Transaction) null, T0);

        assertThat(out.isEmpty()).isTrue();
    }

    private Transaction send(String transactionId, Instant at) {
        Transaction tx = new Transaction(transactionId, "acc-1", TransactionType.DEPOSIT, new BigDecimal("10"), "EUR", at);
        in.pipeInput("acc-1", tx, at);
        return tx;
    }
}
