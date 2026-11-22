package com.javabanker.exercises.ex09;

import com.javabanker.exercises.ex09.Statements.Merchant;
import com.javabanker.exercises.ex09.Statements.MerchantClient;
import com.javabanker.exercises.ex09.Statements.StatementLine;
import com.javabanker.exercises.ex09.Statements.TransactionRepository;
import com.javabanker.exercises.ex09.Statements.TransactionRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Reference solution: collect the distinct merchant ids, load them with as few bulk calls as the API
 * limit allows, then join in memory. 500 transactions over 40 merchants = 1 remote call instead of 500.
 * Same idea as fixing JPA N+1 with a fetch join, @EntityGraph or @BatchSize.
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
        List<TransactionRow> rows = transactions.findByAccountId(accountId);
        Map<String, Merchant> merchantsById = loadMerchants(rows);
        return rows.stream().map(row -> toLine(row, merchantsById.get(row.merchantId()))).toList();
    }

    private Map<String, Merchant> loadMerchants(List<TransactionRow> rows) {
        List<String> distinctIds = new ArrayList<>(new LinkedHashSet<>(
                rows.stream().map(TransactionRow::merchantId).toList()));
        Map<String, Merchant> result = new HashMap<>();
        for (int from = 0; from < distinctIds.size(); from += MerchantClient.MAX_IDS_PER_REQUEST) {
            int to = Math.min(from + MerchantClient.MAX_IDS_PER_REQUEST, distinctIds.size());
            result.putAll(merchants.findAllByIds(distinctIds.subList(from, to)));
        }
        return result;
    }

    private static StatementLine toLine(TransactionRow row, Merchant merchant) {
        return merchant == null
                ? new StatementLine(row.transactionId(), UNKNOWN, UNKNOWN, row.amount())
                : new StatementLine(row.transactionId(), merchant.name(), merchant.category(), row.amount());
    }
}
