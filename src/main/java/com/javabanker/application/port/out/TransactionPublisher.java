package com.javabanker.application.port.out;

import com.javabanker.domain.Transaction;

/** Outbound port: durably append a transaction to the event log (Kafka in production). */
public interface TransactionPublisher {

    /**
     * @throws PublishFailedException if the transaction could not be stored durably
     */
    void publish(Transaction transaction);

    class PublishFailedException extends RuntimeException {
        public PublishFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
