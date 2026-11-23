package com.javabanker.exercises.ex09;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Types and remote interfaces of exercise 09. */
public final class Statements {

    private Statements() {
    }

    public record TransactionRow(String transactionId, String accountId, String merchantId, BigDecimal amount) {
    }

    public record Merchant(String id, String name, String category) {
    }

    public record StatementLine(String transactionId, String merchantName, String category, BigDecimal amount) {
    }

    public interface TransactionRepository {
        List<TransactionRow> findByAccountId(String accountId);
    }

    /** Remote merchant directory. Every call costs ~50 ms of network latency. */
    public interface MerchantClient {

        /** The bulk endpoint accepts at most this many ids per request. */
        int MAX_IDS_PER_REQUEST = 100;

        Optional<Merchant> findById(String merchantId);

        /** @return the merchants found, keyed by id; unknown ids are simply missing from the map */
        Map<String, Merchant> findAllByIds(Collection<String> merchantIds);
    }
}
