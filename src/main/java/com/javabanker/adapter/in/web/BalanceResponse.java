package com.javabanker.adapter.in.web;

import com.javabanker.domain.AccountBalance;

import java.math.BigDecimal;
import java.time.Instant;

record BalanceResponse(String accountId, BigDecimal balance, String currency, long transactionCount, Instant updatedAt) {

    static BalanceResponse from(AccountBalance b) {
        return new BalanceResponse(b.accountId(), b.balance(), b.currency(), b.transactionCount(), b.updatedAt());
    }
}
