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

    @SuppressWarnings("unchecked")
    public <V> KStream<String, V> stream(String topic, Serde<V> valueSerde) {
        return (KStream<String, V>) streams.computeIfAbsent(
                topic, t -> builder.stream(t, Consumed.with(Serdes.String(), valueSerde)));
    }
}
