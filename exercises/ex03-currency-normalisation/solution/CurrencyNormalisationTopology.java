package com.javabanker.exercises.ex03;

import com.javabanker.domain.Transaction;
import com.javabanker.streams.support.JsonSerdes;
import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reference solution for exercise 03. A GlobalKTable is used because rates are small reference data
 * keyed by currency, while transactions are keyed by accountId: no co-partitioning possible or needed.
 */
public class CurrencyNormalisationTopology implements TopologyContributor {

    public static final String TRANSACTIONS = "bank.transactions.v1";
    public static final String FX_RATES = "bank.fx-rates.v1";
    public static final String NORMALISED = "bank.transactions.eur.v1";
    public static final String UNPRICED = "bank.transactions.unpriced.v1";

    private static final String EUR = "EUR";

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        Serde<Transaction> txSerde = JsonSerdes.of(Transaction.class);
        GlobalKTable<String, FxRate> rates =
                builder.globalTable(FX_RATES, Consumed.with(Serdes.String(), JsonSerdes.of(FxRate.class)));

        sources.stream(TRANSACTIONS, txSerde)
                .filter((accountId, tx) -> tx != null)
                .leftJoin(rates,
                        (accountId, tx) -> tx.currency(),                       // which rate to look up
                        (tx, rate) -> new NormalisedTransaction(tx, toEur(tx, rate)))
                .split(Named.as("fx-"))
                .branch((accountId, n) -> n.amountEur() != null,
                        Branched.withConsumer(priced -> priced.to(NORMALISED,
                                Produced.with(Serdes.String(), JsonSerdes.of(NormalisedTransaction.class))), "priced"))
                .defaultBranch(Branched.withConsumer(unpriced -> unpriced
                        .mapValues(NormalisedTransaction::transaction)
                        .to(UNPRICED, Produced.with(Serdes.String(), txSerde)), "unpriced"));
    }

    private static BigDecimal toEur(Transaction tx, FxRate rate) {
        if (EUR.equals(tx.currency())) {
            return tx.amount().setScale(2, RoundingMode.HALF_EVEN);
        }
        if (rate == null) {
            return null;
        }
        return tx.amount().multiply(rate.rateToEur()).setScale(2, RoundingMode.HALF_EVEN);
    }
}
