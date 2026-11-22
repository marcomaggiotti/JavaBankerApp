package com.javabanker.streams.support;

import org.apache.kafka.streams.StreamsBuilder;

/**
 * Extension point of the challenge framework: every feature/challenge is one TopologyContributor.
 * <p>
 * Spring collects all contributor beans and lets each add its sub-topology to the single
 * {@link StreamsBuilder} of the application. In tests you call {@link #contribute} yourself with a
 * fresh builder and run it in a {@code TopologyTestDriver} - no broker needed.
 */
public interface TopologyContributor {

    void contribute(StreamsBuilder builder, SourceStreams sources);
}
