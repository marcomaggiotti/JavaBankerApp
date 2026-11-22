package com.javabanker.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Current state of an account. This is the aggregate root: the only way to change a balance is
 * {@link #apply(Transaction)}, which enforces the business rules (no overdraft, single currency).
 * <p>
 * The class is deliberately free of Kafka/Spring so the rules are testable in milliseconds and the
 * same logic can run inside a Kafka Streams processor, a REST service or a batch job.
 */
public record AccountBalance(
        String accountId,
        BigDecimal balance,
        String currency,
        long transactionCount,
        String lastTransactionId,
        Instant updatedAt) {

    public AccountBalance {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }

    public static AccountBalance open(String accountId, String currency) {
        return new AccountBalance(accountId, BigDecimal.ZERO, currency, 0, null, null);
    }

    public TransactionOutcome apply(Transaction tx) {
        if (!accountId.equals(tx.accountId())) {
            return TransactionOutcome.rejected(tx, this, RejectionReason.WRONG_ACCOUNT);
        }
        if (!currency.equals(tx.currency())) {
            return TransactionOutcome.rejected(tx, this, RejectionReason.CURRENCY_MISMATCH);
        }
        BigDecimal newBalance = balance.add(tx.signedAmount());
        if (newBalance.signum() < 0) {
            return TransactionOutcome.rejected(tx, this, RejectionReason.INSUFFICIENT_FUNDS);
        }
        AccountBalance updated = new AccountBalance(
                accountId, newBalance, currency, transactionCount + 1, tx.transactionId(), tx.timestamp());
        return TransactionOutcome.accepted(tx, updated);
    }
}
