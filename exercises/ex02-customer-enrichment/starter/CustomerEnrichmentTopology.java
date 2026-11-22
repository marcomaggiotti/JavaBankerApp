package com.javabanker.exercises.ex02;

import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.streams.StreamsBuilder;

/** Exercise 02 - see exercises/ex02-customer-enrichment/README.md */
public class CustomerEnrichmentTopology implements TopologyContributor {

    public static final String TRANSACTIONS = "bank.transactions.v1";
    public static final String CUSTOMERS = "bank.customers.v1";
    public static final String OUTPUT = "bank.transactions.enriched.v1";

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        // TODO ex02: read CUSTOMERS as a table, join every transaction with it, write to OUTPUT
        throw new UnsupportedOperationException("TODO ex02");
    }
}
