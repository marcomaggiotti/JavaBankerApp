package com.javabanker.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable fact: "someone asked to move money on an account".
 * <p>
 * Invariants are enforced in the compact constructor, so an invalid Transaction can never exist
 * anywhere in the system (always-valid domain model).
 */
public record Transaction(
        String transactionId,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        Instant timestamp) {

    public Transaction {
        requireText(transactionId, "transactionId");
        requireText(accountId, "accountId");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive but was " + amount);
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must be an ISO-4217 code but was " + currency);
        }
    }

    public BigDecimal signedAmount() {
        return type.signed(amount);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
