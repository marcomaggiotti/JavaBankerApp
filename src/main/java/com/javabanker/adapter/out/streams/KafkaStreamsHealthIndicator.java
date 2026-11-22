package com.javabanker.adapter.out.streams;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Exposed as "kafkaStreams" and included in the readiness group, so Kubernetes stops routing traffic
 * to a pod whose stream threads died or are not yet RUNNING.
 */
@Component("kafkaStreams")
class KafkaStreamsHealthIndicator implements HealthIndicator {

    private final StreamsBuilderFactoryBean factoryBean;

    KafkaStreamsHealthIndicator(StreamsBuilderFactoryBean factoryBean) {
        this.factoryBean = factoryBean;
    }

    @Override
    public Health health() {
        KafkaStreams streams = factoryBean.getKafkaStreams();
        if (streams == null) {
            return Health.down().withDetail("state", "NOT_CREATED").build();
        }
        KafkaStreams.State state = streams.state();
        Health.Builder builder = (state == KafkaStreams.State.RUNNING || state == KafkaStreams.State.REBALANCING)
                ? Health.up() : Health.down();
        return builder.withDetail("state", state.name()).build();
    }
}
