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

    /**
     * Builds the pipeline. Almost every DSL operation takes a <b>lambda</b>: a short anonymous function
     * that Kafka Streams calls once per record. Reading guide for the lambdas below:
     * <ul>
     *   <li>{@code (accountId, tx) -> ...}: two parameters, because a Kafka record has a key and a value.
     *       Their types are inferred by the compiler from the stream type {@code KStream<String, Transaction>}.</li>
     *   <li>{@code filter} expects a {@link org.apache.kafka.streams.kstream.Predicate} (returns boolean).</li>
     *   <li>{@code map} expects a {@link org.apache.kafka.streams.kstream.KeyValueMapper} (returns a new key/value).</li>
     * </ul>
     * Lambdas only <i>describe</i> the processing here; nothing runs until the topology is started.
     */
    @Override
    public void contribute(StreamsBuilder builder, SourceStreams sources) {
        sources.stream(topics.transactions(), JsonSerdes.of(Transaction.class))
                // Predicate<String, Transaction>: keep a record only when the lambda returns true.
                // "tx != null" comes first: && short-circuits, so tx.type() is never called on null
                // (null values = tombstones or unreadable records).
                .filter((accountId, tx) -> tx != null && tx.type() == TransactionType.WITHDRAWAL)
                .groupByKey(Grouped.with(Serdes.String(), JsonSerdes.of(Transaction.class)))
                .windowedBy(TimeWindows.ofSizeAndGrace(windowSize, grace))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(COUNT_STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                // After windowing the key is a Windowed<String> (account + time window) and the value is the count.
                // The lambda reads the field "threshold": lambdas can use fields and effectively-final
                // variables of the enclosing scope (they "capture" them).
                .filter((window, count) -> count >= threshold)
                // KeyValueMapper: returns a NEW key and value. Here the key goes back from Windowed<String>
                // to the plain accountId. Changing the key with map() would make a later stateful operation
                // repartition; when only the value changes, prefer mapValues() (no repartition).
                .map((window, count) -> KeyValue.pair(window.key(), new FraudAlert(
                        window.key(), RULE, count, window.window().startTime(), window.window().endTime())))
                .to(topics.fraudAlerts(), Produced.with(Serdes.String(), JsonSerdes.of(FraudAlert.class)));
    }
}
