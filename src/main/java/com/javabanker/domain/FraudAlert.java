package com.javabanker.domain;

import java.time.Instant;

/** Emitted when an account shows suspicious activity inside a time window. */
public record FraudAlert(
        String accountId,
        String rule,
        long observedCount,
        Instant windowStart,
        Instant windowEnd) {
}
