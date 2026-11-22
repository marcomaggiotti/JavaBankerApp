package com.javabanker.support;

import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;
import com.javabanker.streams.support.JsonSerdes;
import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

/**
 * Base class for testing any {@link TopologyContributor} with {@link TopologyTestDriver}:
 * no broker, no Docker, deterministic time. Typical test:
 *
 * <pre>{@code
 * start(new MyTopology(topics));
 * var in  = input("in-topic", Transaction.class);
 * var out = output("out-topic", MyResult.class);
 * in.pipeInput("acc-1", deposit("acc-1", "10.00"), T0);
 * assertThat(out.readValue())...
 * }</pre>
 */
public abstract class TopologyTestSupport {

    protected static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    protected TopologyTestDriver driver;
    protected Topology topology;

    protected void start(TopologyContributor... contributors) {
        StreamsBuilder builder = new StreamsBuilder();
        SourceStreams sources = new SourceStreams(builder);
        for (TopologyContributor contributor : contributors) {
            contributor.contribute(builder, sources);
        }
        topology = builder.build();
        driver = new TopologyTestDriver(topology, testProperties(), T0);
    }

    protected Properties testProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-" + getClass().getSimpleName());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0); // emit every update, easier to assert
        return props;
    }

    protected <V> TestInputTopic<String, V> input(String topic, Class<V> type) {
        return driver.createInputTopic(topic, Serdes.String().serializer(), JsonSerdes.<V>serializer());
    }

    protected <V> TestOutputTopic<String, V> output(String topic, Class<V> type) {
        return driver.createOutputTopic(topic, Serdes.String().deserializer(), JsonSerdes.deserializer(type));
    }

    @AfterEach
    void closeDriver() {
        if (driver != null) {
            driver.close(); // also wipes the state directory
        }
    }

    // ---- test data builders -------------------------------------------------------------------

    protected static Transaction deposit(String accountId, String amount) {
        return tx(accountId, TransactionType.DEPOSIT, amount, T0);
    }

    protected static Transaction withdrawal(String accountId, String amount) {
        return tx(accountId, TransactionType.WITHDRAWAL, amount, T0);
    }

    protected static Transaction tx(String accountId, TransactionType type, String amount, Instant at) {
        return new Transaction(UUID.randomUUID().toString(), accountId, type, new BigDecimal(amount), "EUR", at);
    }
}
