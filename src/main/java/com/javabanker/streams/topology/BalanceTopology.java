package com.javabanker.streams.topology;

import com.javabanker.domain.AccountBalance;
import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionOutcome;
import com.javabanker.streams.support.JsonSerdes;
import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import com.javabanker.streams.support.Topics;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.Stores;

/**
 * Solved challenge #1 - account balances with overdraft protection.
 *
 * <pre>
 *  transactions --process(BalanceProcessor + balances-store)--> outcomes
 *        outcomes --accepted--> balances topic  (compacted: latest balance per account)
 *        outcomes --rejected--> rejected topic
 * </pre>
 *
 * Contract: input records are keyed by accountId, so all transactions of an account land in the same
 * partition and are processed in order by one task. (If not, add {@code .repartition()}.)
 */
public class BalanceTopology implements TopologyContributor {

    public static final String BALANCE_STORE = "balances-store";

    private final Topics topics;
    private final Serde<Transaction> transactionSerde = JsonSerdes.of(Transaction.class);
    private final Serde<AccountBalance> balanceSerde = JsonSerdes.of(AccountBalance.class);
    private final Serde<TransactionOutcome> outcomeSerde = JsonSerdes.of(TransactionOutcome.class);

    public BalanceTopology(Topics topics) {
        this.topics = topics;
    }

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        builder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(BALANCE_STORE), Serdes.String(), balanceSerde));

        KStream<String, TransactionOutcome> outcomes = sources
                .stream(topics.transactions(), transactionSerde)
                .process(() -> new BalanceProcessor(BALANCE_STORE), Named.as("apply-transaction"), BALANCE_STORE);

        outcomes.split(Named.as("outcome-"))
                .branch((accountId, outcome) -> outcome.isAccepted(),
                        Branched.withConsumer(accepted -> accepted
                                .mapValues(TransactionOutcome::balance)
                                .to(topics.balances(), Produced.with(Serdes.String(), balanceSerde)),
                                "accepted"))
                .defaultBranch(Branched.withConsumer(rejected -> rejected
                                .to(topics.rejected(), Produced.with(Serdes.String(), outcomeSerde)),
                        "rejected"));
    }
}
