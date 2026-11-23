package com.javabanker.exercises.ex06;

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

/** Reference solution for exercise 06: Processor API + wall-clock punctuator. */
public class DormantAccountTopology implements TopologyContributor {

    public static final String INPUT = "bank.transactions.v1";
    public static final String OUTPUT = "bank.dormant-accounts.v1";
    private static final String STORE = "account-activity-store";

    private final Duration inactivity;
    private final Duration checkInterval;

    public DormantAccountTopology(Duration inactivity, Duration checkInterval) {
        this.inactivity = inactivity;
        this.checkInterval = checkInterval;
    }

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        builder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STORE), Serdes.String(), JsonSerdes.of(AccountActivity.class)));

        sources.stream(INPUT, JsonSerdes.of(Transaction.class))
                .process(() -> new DormantAccountProcessor(STORE, inactivity, checkInterval),
                        Named.as("detect-dormant"), STORE)
                .to(OUTPUT, Produced.with(Serdes.String(), JsonSerdes.of(DormantAccountAlert.class)));
    }
}
