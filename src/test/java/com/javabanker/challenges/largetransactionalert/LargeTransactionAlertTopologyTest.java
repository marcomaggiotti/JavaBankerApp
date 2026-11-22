package com.javabanker.challenges.largetransactionalert;

import com.javabanker.domain.Transaction;
import com.javabanker.support.TopologyTestSupport;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LargeTransactionAlertTopologyTest extends TopologyTestSupport {

    private TestInputTopic<String, Transaction> in;
    private TestOutputTopic<String, Transaction> out;

    @BeforeEach
    void setUp() {
        start(new LargeTransactionAlertTopology());
        in = input(LargeTransactionAlertTopology.INPUT_TOPIC, Transaction.class);
        out = output(LargeTransactionAlertTopology.OUTPUT_TOPIC, Transaction.class);
    }

    @Test
    void forwardsTransactionsAtOrAboveThreshold() {
        Transaction exactlyThreshold = deposit("acc-1", "10000.00");
        Transaction above = withdrawal("acc-2", "25000");

        in.pipeInput("acc-1", exactlyThreshold);
        in.pipeInput("acc-2", above);

        assertThat(out.readKeyValuesToList())
                .extracting(kv -> kv.key, kv -> kv.value.transactionId())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("acc-1", exactlyThreshold.transactionId()),
                        org.assertj.core.groups.Tuple.tuple("acc-2", above.transactionId()));
    }

    @Test
    void dropsSmallTransactionsAndTombstones() {
        in.pipeInput("acc-1", deposit("acc-1", "9999.99"));
        in.pipeInput("acc-1", (Transaction) null);

        assertThat(out.isEmpty()).isTrue();
    }
}
