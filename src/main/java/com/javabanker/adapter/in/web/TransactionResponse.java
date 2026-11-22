package com.javabanker.adapter.in.web;

import com.javabanker.domain.Transaction;

import java.time.Instant;

record TransactionResponse(String transactionId, String accountId, String status, Instant submittedAt) {

    static TransactionResponse pending(Transaction tx) {
        return new TransactionResponse(tx.transactionId(), tx.accountId(), "PENDING", tx.timestamp());
    }
}
