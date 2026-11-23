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

    /**
     * Builds the balance pipeline. It shows three kinds of lambda:
     * <ol>
     *   <li>a <b>supplier</b> {@code () -> new BalanceProcessor(..)}: no parameters, returns a new object;</li>
     *   <li>a <b>predicate</b> {@code (accountId, outcome) -> outcome.isAccepted()}: returns a boolean;</li>
     *   <li>a <b>consumer</b> {@code accepted -> accepted...}: receives a value and returns nothing.</li>
     * </ol>
     * plus a <b>method reference</b> {@code TransactionOutcome::balance}, a shorter way to write the
     * lambda {@code outcome -> outcome.balance()}.
     */
    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        builder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(BALANCE_STORE), Serdes.String(), balanceSerde));

        KStream<String, TransactionOutcome> outcomes = sources
                .stream(topics.transactions(), transactionSerde)
                // ProcessorSupplier as a lambda: Kafka Streams calls it once PER TASK (one per partition),
                // so every task gets its own BalanceProcessor instance with its own state. Passing one shared
                // "new BalanceProcessor(..)" object instead would be a bug: tasks would share mutable state.
                .process(() -> new BalanceProcessor(BALANCE_STORE), Named.as("apply-transaction"), BALANCE_STORE);

        outcomes.split(Named.as("outcome-"))
                // Predicate: records for which it returns true go to this branch, the rest continue below.
                .branch((accountId, outcome) -> outcome.isAccepted(),
                        // Consumer<KStream>: the lambda receives the branch's sub-stream and says where it goes.
                        Branched.withConsumer(accepted -> accepted
                                // Method reference = "outcome -> outcome.balance()". mapValues keeps the key.
                                .mapValues(TransactionOutcome::balance)
                                .to(topics.balances(), Produced.with(Serdes.String(), balanceSerde)),
                                "accepted"))
                // Everything the predicate rejected: the parameter name "rejected" is our choice, any name works.
                .defaultBranch(Branched.withConsumer(rejected -> rejected
                                .to(topics.rejected(), Produced.with(Serdes.String(), outcomeSerde)),
                        "rejected"));
    }
}
