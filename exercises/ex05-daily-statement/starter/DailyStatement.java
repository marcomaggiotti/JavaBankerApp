package com.javabanker.exercises.ex05;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One statement per account and UTC calendar day. net = totalDeposits - totalWithdrawals. */
public record DailyStatement(
        String accountId,
        LocalDate day,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        long transactionCount,
        BigDecimal net) {
}
