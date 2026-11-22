package com.javabanker.streams.topology;

import com.javabanker.domain.AccountBalance;
import com.javabanker.domain.RejectionReason;
import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionOutcome;
import com.javabanker.streams.support.Topics;
import com.javabanker.support.TopologyTestSupport;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceTopologyTest extends TopologyTestSupport {

    private final Topics topics = Topics.defaults();
    private TestInputTopic<String, Transaction> transactions;
    private TestOutputTopic<String, AccountBalance> balances;
    private TestOutputTopic<String, TransactionOutcome> rejected;

    @BeforeEach
    void setUp() {
        start(new BalanceTopology(topics));
        transactions = input(topics.transactions(), Transaction.class);
        balances = output(topics.balances(), AccountBalance.class);
        rejected = output(topics.rejected(), TransactionOutcome.class);
    }

    @Test
    void publishesRunningBalancePerAccount() {
        transactions.pipeInput("acc-1", deposit("acc-1", "100"));
        transactions.pipeInput("acc-2", deposit("acc-2", "5"));
        transactions.pipeInput("acc-1", withdrawal("acc-1", "30"));

        var latest = balances.readKeyValuesToMap();
        assertThat(latest.get("acc-1").balance()).isEqualByComparingTo("70");
        assertThat(latest.get("acc-2").balance()).isEqualByComparingTo("5");
        assertThat(rejected.isEmpty()).isTrue();
    }

    @Test
    void overdraftGoesToRejectedTopicAndDoesNotChangeState() {
        transactions.pipeInput("acc-1", deposit("acc-1", "10"));
        transactions.pipeInput("acc-1", withdrawal("acc-1", "11"));

        assertThat(balances.readValuesToList()).hasSize(1);
        TransactionOutcome outcome = rejected.readValue();
        assertThat(outcome.reason()).isEqualTo(RejectionReason.INSUFFICIENT_FUNDS);

        KeyValueStore<String, AccountBalance> store = driver.getKeyValueStore(BalanceTopology.BALANCE_STORE);
        assertThat(store.get("acc-1").balance()).isEqualByComparingTo("10");
    }

    @Test
    void topologyDescriptionIsPrintable() {
        // Handy in an interview: paste into https://zz85.github.io/kafka-streams-viz/
        assertThat(topology.describe().toString()).contains("apply-transaction", BalanceTopology.BALANCE_STORE);
    }
}
