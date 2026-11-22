package com.javabanker.exercises.ex02;

import com.javabanker.domain.Transaction;
import com.javabanker.streams.support.JsonSerdes;
import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

/** Reference solution for exercise 02: KStream-KTable left join (both topics keyed by accountId). */
public class CustomerEnrichmentTopology implements TopologyContributor {

    public static final String TRANSACTIONS = "bank.transactions.v1";
    public static final String CUSTOMERS = "bank.customers.v1";
    public static final String OUTPUT = "bank.transactions.enriched.v1";

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        Serde<Transaction> txSerde = JsonSerdes.of(Transaction.class);
        Serde<Customer> customerSerde = JsonSerdes.of(Customer.class);

        // A null value (tombstone) on the compacted topic deletes the customer from the table.
        KTable<String, Customer> customers = builder.table(CUSTOMERS, Consumed.with(Serdes.String(), customerSerde));

        sources.stream(TRANSACTIONS, txSerde)
                .filter((accountId, tx) -> tx != null)
                .leftJoin(customers, CustomerEnrichmentTopology::enrich,
                        Joined.with(Serdes.String(), txSerde, customerSerde))
                .to(OUTPUT, Produced.with(Serdes.String(), JsonSerdes.of(EnrichedTransaction.class)));
    }

    private static EnrichedTransaction enrich(Transaction tx, Customer customer) {
        if (customer == null) {
            return new EnrichedTransaction(tx, null, EnrichedTransaction.UNKNOWN_SEGMENT, null);
        }
        return new EnrichedTransaction(tx, customer.name(), customer.segment(), customer.country());
    }
}
