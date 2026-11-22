package com.javabanker.config;

import com.javabanker.domain.Transaction;
import com.javabanker.streams.support.JsonSerdes;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/** Producer + topic provisioning. In real platforms topics are often owned by GitOps (Strimzi KafkaTopic CRs). */
@Configuration(proxyBeanMethods = false)
public class KafkaConfig {

    @Bean
    ProducerFactory<String, Transaction> transactionProducerFactory(KafkaProperties kafka,
                                                                    ObjectProvider<SslBundles> sslBundles) {
        return new DefaultKafkaProducerFactory<>(
                kafka.buildProducerProperties(sslBundles.getIfAvailable()),
                new StringSerializer(),
                JsonSerdes.serializer());
    }

    @Bean
    KafkaTemplate<String, Transaction> transactionKafkaTemplate(ProducerFactory<String, Transaction> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    KafkaAdmin.NewTopics bankerTopics(BankerProperties props) {
        var t = props.topics();
        var s = props.topicSettings();
        return new KafkaAdmin.NewTopics(
                topic(t.transactions(), s).build(),
                topic(t.balances(), s).config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT).build(),
                topic(t.rejected(), s).build(),
                topic(t.fraudAlerts(), s).build());
    }

    private static TopicBuilder topic(String name, BankerProperties.TopicSettings s) {
        return TopicBuilder.name(name).partitions(s.partitions()).replicas(s.replicationFactor());
    }

}
