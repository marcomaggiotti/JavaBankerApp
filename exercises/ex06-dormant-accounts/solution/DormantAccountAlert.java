package com.javabanker.exercises.ex06;

import java.time.Instant;

/** Emitted once when an account becomes dormant. */
public record DormantAccountAlert(String accountId, Instant lastActivity, Instant detectedAt) {
}
