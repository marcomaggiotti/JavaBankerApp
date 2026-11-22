package com.javabanker.domain;

import java.util.Objects;

/**
 * Result of applying a transaction to a balance. Modelled as a value instead of an exception because
 * a rejected transaction is an expected business event that must be published downstream.
 *
 * @param balance the balance after the transaction (accepted) or the unchanged balance (rejected)
 * @param reason  null when accepted
 */
public record TransactionOutcome(Transaction transaction, AccountBalance balance, RejectionReason reason) {

    public TransactionOutcome {
        Objects.requireNonNull(transaction, "transaction must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
    }

    public static TransactionOutcome accepted(Transaction tx, AccountBalance newBalance) {
        return new TransactionOutcome(tx, newBalance, null);
    }

    public static TransactionOutcome rejected(Transaction tx, AccountBalance unchanged, RejectionReason reason) {
        return new TransactionOutcome(tx, unchanged, Objects.requireNonNull(reason));
    }

    public boolean isAccepted() {
        return reason == null;
    }
}
