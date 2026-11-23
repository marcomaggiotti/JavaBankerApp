package com.javabanker.exercises.ex09;

import com.javabanker.exercises.ex09.Statements.Merchant;
import com.javabanker.exercises.ex09.Statements.MerchantClient;
import com.javabanker.exercises.ex09.Statements.StatementLine;
import com.javabanker.exercises.ex09.Statements.TransactionRow;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("exercise")
class StatementServiceTest {

    private final CountingMerchantClient merchants = new CountingMerchantClient();

    @Test
    void producesOneLinePerTransactionInOrder() {
        List<StatementLine> lines = service(List.of(row("t1", "m1"), row("t2", "m2"), row("t3", "m1"))).statement("acc-1");

        assertThat(lines).extracting(StatementLine::transactionId).containsExactly("t1", "t2", "t3");
        assertThat(lines).extracting(StatementLine::merchantName).containsExactly("Merchant m1", "Merchant m2", "Merchant m1");
        assertThat(lines.get(0).category()).isEqualTo("GROCERIES");
    }

    @Test
    void unknownMerchantIsMarked() {
        StatementLine line = service(List.of(row("t1", "gone"))).statement("acc-1").get(0);

        assertThat(line.merchantName()).isEqualTo("UNKNOWN");
        assertThat(line.category()).isEqualTo("UNKNOWN");
    }

    @Test
    void usesOneBulkCallInsteadOfOnePerTransaction() {
        List<TransactionRow> rows = IntStream.range(0, 50).mapToObj(i -> row("t" + i, "m" + (i % 5))).toList();

        service(rows).statement("acc-1");

        assertThat(merchants.singleCalls).as("findById calls").isZero();
        assertThat(merchants.bulkCalls).hasSize(1);
        assertThat(merchants.bulkCalls.get(0)).containsExactlyInAnyOrder("m0", "m1", "m2", "m3", "m4");
    }

    @Test
    void respectsTheBulkApiLimit() {
        List<TransactionRow> rows = IntStream.range(0, 250).mapToObj(i -> row("t" + i, "m" + i)).toList();

        List<StatementLine> lines = service(rows).statement("acc-1");

        assertThat(lines).hasSize(250).noneMatch(l -> l.merchantName().equals("UNKNOWN"));
        assertThat(merchants.bulkCalls).hasSize(3).allSatisfy(ids -> assertThat(ids).hasSizeLessThanOrEqualTo(100));
        assertThat(merchants.bulkCalls.stream().mapToInt(List::size).sum()).isEqualTo(250);
    }

    @Test
    void emptyStatementMakesNoRemoteCall() {
        assertThat(service(List.of()).statement("acc-1")).isEmpty();
        assertThat(merchants.bulkCalls).isEmpty();
        assertThat(merchants.singleCalls).isZero();
    }

    private StatementService service(List<TransactionRow> rows) {
        return new StatementService(accountId -> rows, merchants);
    }

    private static TransactionRow row(String id, String merchantId) {
        return new TransactionRow(id, "acc-1", merchantId, new BigDecimal("9.99"));
    }

    /** Fake remote directory: knows every id except "gone"; rejects requests above the API limit. */
    static class CountingMerchantClient implements MerchantClient {
        int singleCalls;
        final List<List<String>> bulkCalls = new ArrayList<>();

        @Override
        public Optional<Merchant> findById(String merchantId) {
            singleCalls++;
            return Optional.ofNullable(lookup(merchantId));
        }

        @Override
        public Map<String, Merchant> findAllByIds(Collection<String> merchantIds) {
            if (merchantIds.size() > MAX_IDS_PER_REQUEST) {
                throw new IllegalArgumentException("API limit: at most " + MAX_IDS_PER_REQUEST + " ids per request");
            }
            bulkCalls.add(List.copyOf(merchantIds));
            return merchantIds.stream().map(CountingMerchantClient::lookup).filter(m -> m != null)
                    .collect(Collectors.toMap(Merchant::id, Function.identity()));
        }

        private static Merchant lookup(String id) {
            return id.equals("gone") ? null : new Merchant(id, "Merchant " + id, "GROCERIES");
        }
    }
}
