package com.javabanker.application.port.in;

import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;

import java.math.BigDecimal;

/** Inbound port: accept a transaction request and hand it to the event log. */
public interface SubmitTransactionUseCase {

    Transaction submit(SubmitTransactionCommand command);

    record SubmitTransactionCommand(String accountId, TransactionType type, BigDecimal amount, String currency) {
    }
}
