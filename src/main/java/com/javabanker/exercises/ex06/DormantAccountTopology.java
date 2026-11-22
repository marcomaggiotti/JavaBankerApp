package com.javabanker.exercises.ex06;

import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.streams.StreamsBuilder;

import java.time.Duration;

/** Exercise 06 - see exercises/ex06-dormant-accounts/README.md */
public class DormantAccountTopology implements TopologyContributor {

    public static final String INPUT = "bank.transactions.v1";
    public static final String OUTPUT = "bank.dormant-accounts.v1";

    private final Duration inactivity;
    private final Duration checkInterval;

    public DormantAccountTopology(Duration inactivity, Duration checkInterval) {
        this.inactivity = inactivity;
        this.checkInterval = checkInterval;
    }

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        // TODO ex06: remember last activity per account, check periodically (wall clock), emit alerts once
        throw new UnsupportedOperationException("TODO ex06");
    }
}
