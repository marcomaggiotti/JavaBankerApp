package com.javabanker.streams.support;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Streams forbids subscribing to the same topic twice in one topology
 * ("Topic has already been registered by another source"). Several contributors read the same
 * input topic, so source streams are created once and shared through this registry.
 */
public final class SourceStreams {

    private final StreamsBuilder builder;
    private final Map<String, KStream<String, ?>> streams = new HashMap<>();

    public SourceStreams(StreamsBuilder builder) {
        this.builder = builder;
    }

    /**
     * Returns the shared stream for {@code topic}, creating it on the first call.
     * <p>
     * {@link Map#computeIfAbsent} takes a {@code Function<String, KStream>}: the lambda {@code t -> ...}
     * is called <b>only if the key is missing</b>, and its result is stored in the map. This replaces
     * the classic "check, create, put" pattern:
     * <pre>{@code
     * KStream<String, ?> stream = streams.get(topic);
     * if (stream == null) {
     *     stream = builder.stream(topic, ...);
     *     streams.put(topic, stream);
     * }
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <V> KStream<String, V> stream(String topic, Serde<V> valueSerde) {
        return (KStream<String, V>) streams.computeIfAbsent(
                // "t" is the missing key (equal to topic). Using the parameter rather than capturing "topic"
                // is the idiomatic style, and avoids accidentally capturing the wrong variable.
                topic, t -> builder.stream(t, Consumed.with(Serdes.String(), valueSerde)));
    }
}
