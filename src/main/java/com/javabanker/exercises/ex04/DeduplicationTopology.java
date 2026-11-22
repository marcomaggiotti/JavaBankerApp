package com.javabanker.exercises.ex04;

import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.streams.StreamsBuilder;

import java.time.Duration;

/** Exercise 04 - see exercises/ex04-deduplication/README.md */
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
        // TODO ex04: register a state store, process() with DeduplicationProcessor, write to OUTPUT
        throw new UnsupportedOperationException("TODO ex04");
    }
}
