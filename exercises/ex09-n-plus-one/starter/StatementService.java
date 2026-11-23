package com.javabanker.exercises.ex09;

import com.javabanker.exercises.ex09.Statements.MerchantClient;
import com.javabanker.exercises.ex09.Statements.StatementLine;
import com.javabanker.exercises.ex09.Statements.TransactionRepository;

import java.util.List;

/**
 * Exercise 09 - correct, but makes one remote call PER TRANSACTION (the N+1 problem):
 * 500 transactions = 501 round trips. See exercises/ex09-n-plus-one/README.md
 */
public class StatementService {

    static final String UNKNOWN = "UNKNOWN";

    private final TransactionRepository transactions;
    private final MerchantClient merchants;

    public StatementService(TransactionRepository transactions, MerchantClient merchants) {
        this.transactions = transactions;
        this.merchants = merchants;
    }

    public List<StatementLine> statement(String accountId) {
        return transactions.findByAccountId(accountId).stream()
                .map(row -> merchants.findById(row.merchantId())
                        .map(m -> new StatementLine(row.transactionId(), m.name(), m.category(), row.amount()))
                        .orElseGet(() -> new StatementLine(row.transactionId(), UNKNOWN, UNKNOWN, row.amount())))
                .toList();
    }
}
