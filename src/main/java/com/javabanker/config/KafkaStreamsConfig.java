package com.javabanker.config;

import com.javabanker.streams.support.SourceStreams;
import com.javabanker.streams.support.TopologyContributor;
import com.javabanker.streams.topology.BalanceTopology;
import com.javabanker.streams.topology.FraudDetectionTopology;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

import java.util.List;

/**
 * Wires every {@link TopologyContributor} into the single StreamsBuilder managed by Spring Kafka.
 * To add a challenge: create a contributor (see scripts/new-challenge.sh) and register it as a bean.
 */
@Configuration(proxyBeanMethods = false)
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean
    BalanceTopology balanceTopology(BankerProperties props) {
        return new BalanceTopology(props.topics());
    }

    @Bean
    FraudDetectionTopology fraudDetectionTopology(BankerProperties props) {
        var fraud = props.fraud();
        return new FraudDetectionTopology(props.topics(), fraud.withdrawalThreshold(), fraud.window(), fraud.grace());
    }

    @Bean
    SourceStreams bankerTopology(StreamsBuilder builder, List<TopologyContributor> contributors) {
        SourceStreams sources = new SourceStreams(builder);
        // Iterable.forEach takes a Consumer<TopologyContributor>: the lambda runs once per element.
        // It is the functional form of: for (TopologyContributor c : contributors) { c.contribute(builder, sources); }
        // "builder" and "sources" are local variables captured by the lambda; this is allowed because they
        // are effectively final (assigned once). Reassigning them afterwards would not compile.
        contributors.forEach(contributor -> contributor.contribute(builder, sources));
        return sources;
    }
}
