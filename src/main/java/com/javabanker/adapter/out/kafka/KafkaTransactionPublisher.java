package com.javabanker.adapter.out.kafka;

import com.javabanker.application.port.out.TransactionPublisher;
import com.javabanker.config.BankerProperties;
import com.javabanker.domain.Transaction;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Driven adapter: publishes to Kafka keyed by accountId (ordering per account!). We wait for the broker
 * ack (acks=all, idempotent producer) so a 202 response really means "durably stored".
 */
@Component
class KafkaTransactionPublisher implements TransactionPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, Transaction> template;
    private final String topic;

    KafkaTransactionPublisher(KafkaTemplate<String, Transaction> template, BankerProperties props) {
        this.template = template;
        this.topic = props.topics().transactions();
    }

    @Override
    public void publish(Transaction tx) {
        try {
            template.send(topic, tx.accountId(), tx).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublishFailedException("Interrupted while publishing " + tx.transactionId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new PublishFailedException("Could not publish " + tx.transactionId(), e);
        }
    }
}
