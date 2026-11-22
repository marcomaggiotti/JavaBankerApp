package com.javabanker.challenges.largetransactionalert;

import com.javabanker.domain.Transaction;
import com.javabanker.streams.support.JsonSerdes;
import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;

import java.math.BigDecimal;

/**
 * Challenge: LargeTransactionAlert (warm-up, generated with scripts/new-challenge.sh then solved).
 * <p>
 * Statement: "Publish every transaction whose amount is at or above a configurable threshold
 * (default 10 000) to an AML-review topic."
 * <ul>
 *   <li>Input:  bank.transactions.v1 (key = accountId, value = Transaction JSON)</li>
 *   <li>Output: bank.large-transactions.v1 (same key, same value)</li>
 * </ul>
 * Assumptions: single currency (multi-currency => see challenge 4, GlobalKTable FX join);
 * stateless, so no repartition and no state store; null values (tombstones) are dropped.
 */
public class LargeTransactionAlertTopology implements TopologyContributor {

    public static final String INPUT_TOPIC = "bank.transactions.v1";
    public static final String OUTPUT_TOPIC = "bank.large-transactions.v1";
    public static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("10000");

    private final BigDecimal threshold;

    public LargeTransactionAlertTopology() {
        this(DEFAULT_THRESHOLD);
    }

    public LargeTransactionAlertTopology(BigDecimal threshold) {
        this.threshold = threshold;
    }

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        sources.stream(INPUT_TOPIC, JsonSerdes.of(Transaction.class))
                .filter((accountId, tx) -> tx != null && tx.amount().compareTo(threshold) >= 0,
                        Named.as("large-transaction-filter"))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), JsonSerdes.of(Transaction.class)));
    }
}
