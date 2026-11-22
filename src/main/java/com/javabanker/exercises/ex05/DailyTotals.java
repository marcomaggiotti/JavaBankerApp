package com.javabanker.exercises.ex05;

import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;

import java.math.BigDecimal;

/** Intermediate aggregate kept in the window store. */
record DailyTotals(BigDecimal deposits, BigDecimal withdrawals, long count) {

    static DailyTotals empty() {
        return new DailyTotals(BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }

    DailyTotals add(Transaction tx) {
        return tx.type() == TransactionType.DEPOSIT
                ? new DailyTotals(deposits.add(tx.amount()), withdrawals, count + 1)
                : new DailyTotals(deposits, withdrawals.add(tx.amount()), count + 1);
    }
}
