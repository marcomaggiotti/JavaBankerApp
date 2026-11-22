package com.javabanker.exercises.ex02;

import com.javabanker.domain.Transaction;

/** Output value. For an unknown customer: name and country are null, segment is UNKNOWN_SEGMENT. */
public record EnrichedTransaction(Transaction transaction, String customerName, String segment, String country) {

    public static final String UNKNOWN_SEGMENT = "UNKNOWN";
}
