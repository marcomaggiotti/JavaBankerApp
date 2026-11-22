package com.javabanker.exercises.ex03;

import com.javabanker.domain.Transaction;

import java.math.BigDecimal;

/** Output value: the original transaction plus its amount in EUR (scale 2, HALF_EVEN). */
public record NormalisedTransaction(Transaction transaction, BigDecimal amountEur) {
}
