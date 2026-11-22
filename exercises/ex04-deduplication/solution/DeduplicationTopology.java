package com.javabanker.exercises.ex04;

import com.javabanker.domain.Transaction;
import com.javabanker.streams.support.JsonSerdes;
import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.Stores;

import java.time.Duration;

/**
 * Reference solution for exercise 04. A window store is used as a TTL cache: entries older than the
 * retention are dropped segment by segment, so state does not grow forever.
 */
public class DeduplicationTopology implements TopologyContributor {

    public static final String INPUT = "bank.transactions.v1";
    public static final String OUTPUT = "bank.transactions.deduplicated.v1";
    public static final String STORE = "dedup-store";

    private final Duration window;

    public DeduplicationTopology(Duration window) {
        this.window = window;
    }

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        builder.addStateStore(Stores.windowStoreBuilder(
                Stores.persistentWindowStore(STORE, window, window, false),
                Serdes.String(), Serdes.Long()));

        sources.stream(INPUT, JsonSerdes.of(Transaction.class))
                .process(() -> new DeduplicationProcessor(STORE, window), Named.as("deduplicate"), STORE)
                .to(OUTPUT, Produced.with(Serdes.String(), JsonSerdes.of(Transaction.class)));
    }
}
