package com.javabanker.exercises.ex05;

import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.streams.StreamsBuilder;

import java.time.Duration;

/** Exercise 05 - see exercises/ex05-daily-statement/README.md */
public class DailyStatementTopology implements TopologyContributor {

    public static final String INPUT = "bank.transactions.v1";
    public static final String OUTPUT = "bank.daily-statements.v1";

    private final Duration grace;

    public DailyStatementTopology(Duration grace) {
        this.grace = grace;
    }

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        // TODO ex05: daily windows per account, aggregate totals, emit ONE final statement per day
        throw new UnsupportedOperationException("TODO ex05");
    }
}
