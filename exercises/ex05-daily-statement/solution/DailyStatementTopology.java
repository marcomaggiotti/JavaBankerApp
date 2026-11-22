package com.javabanker.exercises.ex05;

import com.javabanker.domain.Transaction;
import com.javabanker.streams.support.JsonSerdes;
import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Reference solution for exercise 05: tumbling 1-day windows (aligned to UTC midnight) + suppress. */
public class DailyStatementTopology implements TopologyContributor {

    public static final String INPUT = "bank.transactions.v1";
    public static final String OUTPUT = "bank.daily-statements.v1";

    private final Duration grace;

    public DailyStatementTopology(Duration grace) {
        this.grace = grace;
    }

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        Serde<Transaction> txSerde = JsonSerdes.of(Transaction.class);

        sources.stream(INPUT, txSerde)
                .filter((accountId, tx) -> tx != null)
                .groupByKey(Grouped.with(Serdes.String(), txSerde))
                .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofDays(1), grace))
                .aggregate(DailyTotals::empty,
                        (accountId, tx, totals) -> totals.add(tx),
                        Materialized.<String, DailyTotals, WindowStore<Bytes, byte[]>>as("daily-totals-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(JsonSerdes.of(DailyTotals.class)))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((window, totals) -> KeyValue.pair(window.key(), toStatement(window, totals)))
                .to(OUTPUT, Produced.with(Serdes.String(), JsonSerdes.of(DailyStatement.class)));
    }

    private static DailyStatement toStatement(Windowed<String> window, DailyTotals totals) {
        return new DailyStatement(
                window.key(),
                LocalDate.ofInstant(window.window().startTime(), ZoneOffset.UTC),
                totals.deposits(),
                totals.withdrawals(),
                totals.count(),
                totals.deposits().subtract(totals.withdrawals()));
    }
}
