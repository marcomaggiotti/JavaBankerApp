package com.javabanker.streams.topology;

import com.javabanker.domain.FraudAlert;
import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;
import com.javabanker.streams.support.JsonSerdes;
import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import com.javabanker.streams.support.Topics;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;

/**
 * Solved challenge #2 - withdrawal velocity check.
 * "Alert when an account makes at least N withdrawals within a tumbling window of W."
 * <p>
 * Key concepts: event-time windowing, grace period for late events, and {@code suppress} to emit one
 * FINAL result per window instead of every intermediate count update.
 */
public class FraudDetectionTopology implements TopologyContributor {

    public static final String RULE = "WITHDRAWAL_VELOCITY";
    public static final String COUNT_STORE = "withdrawal-counts-store";

    private final Topics topics;
    private final long threshold;
    private final Duration windowSize;
    private final Duration grace;

    public FraudDetectionTopology(Topics topics, long threshold, Duration windowSize, Duration grace) {
        this.topics = topics;
        this.threshold = threshold;
        this.windowSize = windowSize;
        this.grace = grace;
    }

    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        sources.stream(topics.transactions(), JsonSerdes.of(Transaction.class))
                .filter((accountId, tx) -> tx != null && tx.type() == TransactionType.WITHDRAWAL)
                .groupByKey(Grouped.with(Serdes.String(), JsonSerdes.of(Transaction.class)))
                .windowedBy(TimeWindows.ofSizeAndGrace(windowSize, grace))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(COUNT_STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .filter((window, count) -> count >= threshold)
                .map((window, count) -> KeyValue.pair(window.key(), new FraudAlert(
                        window.key(), RULE, count, window.window().startTime(), window.window().endTime())))
                .to(topics.fraudAlerts(), Produced.with(Serdes.String(), JsonSerdes.of(FraudAlert.class)));
    }
}
