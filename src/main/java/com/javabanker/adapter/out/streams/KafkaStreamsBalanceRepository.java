package com.javabanker.adapter.out.streams;

import com.javabanker.application.port.out.BalanceRepository;
import com.javabanker.domain.AccountBalance;
import com.javabanker.streams.topology.BalanceTopology;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Interactive Queries: reads the balance directly from the local RocksDB state store.
 * <p>
 * Limitation worth discussing in an interview: with N instances each one only holds the partitions it
 * owns. A production version uses {@code KafkaStreams#queryMetadataForKey} to find the owning host
 * (application.server config) and forwards the HTTP call there.
 */
@Component
class KafkaStreamsBalanceRepository implements BalanceRepository {

    private final StreamsBuilderFactoryBean factoryBean;

    KafkaStreamsBalanceRepository(StreamsBuilderFactoryBean factoryBean) {
        this.factoryBean = factoryBean;
    }

    @Override
    public Optional<AccountBalance> findByAccountId(String accountId) {
        KafkaStreams streams = factoryBean.getKafkaStreams();
        if (streams == null) {
            throw new BalanceUnavailableException("Kafka Streams is not started", null);
        }
        try {
            ReadOnlyKeyValueStore<String, AccountBalance> store = streams.store(StoreQueryParameters.fromNameAndType(
                    BalanceTopology.BALANCE_STORE, QueryableStoreTypes.keyValueStore()));
            return Optional.ofNullable(store.get(accountId));
        } catch (InvalidStateStoreException e) {
            throw new BalanceUnavailableException("Balance store not queryable (rebalancing?)", e);
        }
    }
}
