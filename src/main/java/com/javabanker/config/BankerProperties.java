package com.javabanker.config;

import com.javabanker.streams.support.Topics;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Type-safe configuration bound from {@code banker.*} (application.yml / env vars / ConfigMap). */
@ConfigurationProperties("banker")
public record BankerProperties(Topics topics, TopicSettings topicSettings, Fraud fraud) {

    public record TopicSettings(@DefaultValue("3") int partitions, @DefaultValue("1") short replicationFactor) {
    }

    public record Fraud(
            @DefaultValue("3") long withdrawalThreshold,
            @DefaultValue("1m") Duration window,
            @DefaultValue("10s") Duration grace) {
    }
}
