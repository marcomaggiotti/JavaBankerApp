package com.javabanker.application.port.out;

import com.javabanker.domain.AccountBalance;

import java.util.Optional;

/** Outbound port: read model of account balances (a Kafka Streams state store in production). */
public interface BalanceRepository {

    Optional<AccountBalance> findByAccountId(String accountId);

    /** Thrown when the read model is temporarily unavailable (e.g. during a rebalance). */
    class BalanceUnavailableException extends RuntimeException {
        public BalanceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
