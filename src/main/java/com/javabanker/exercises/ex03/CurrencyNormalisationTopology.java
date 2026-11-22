package com.javabanker.exercises.ex03;

import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.streams.StreamsBuilder;

/** Exercise 03 - see exercises/ex03-currency-normalisation/README.md */
public class CurrencyNormalisationTopology implements TopologyContributor {

    public static final String TRANSACTIONS = "bank.transactions.v1";
    public static final String FX_RATES = "bank.fx-rates.v1";
    public static final String NORMALISED = "bank.transactions.eur.v1";
    public static final String UNPRICED = "bank.transactions.unpriced.v1";

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        // TODO ex03: GlobalKTable of FX rates, convert to EUR, route unknown currencies to UNPRICED
        throw new UnsupportedOperationException("TODO ex03");
    }
}
