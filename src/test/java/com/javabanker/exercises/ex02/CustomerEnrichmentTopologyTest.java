package com.javabanker.exercises.ex02;

import com.javabanker.domain.Transaction;
import com.javabanker.support.TopologyTestSupport;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("exercise")
class CustomerEnrichmentTopologyTest extends TopologyTestSupport {

    private TestInputTopic<String, Transaction> transactions;
    private TestInputTopic<String, Customer> customers;
    private TestOutputTopic<String, EnrichedTransaction> enriched;

    @BeforeEach
    void setUp() {
        start(new CustomerEnrichmentTopology());
        transactions = input(CustomerEnrichmentTopology.TRANSACTIONS, Transaction.class);
        customers = input(CustomerEnrichmentTopology.CUSTOMERS, Customer.class);
        enriched = output(CustomerEnrichmentTopology.OUTPUT, EnrichedTransaction.class);
    }

    @Test
    void enrichesKnownCustomer() {
        customers.pipeInput("acc-1", new Customer("acc-1", "Ada", "PREMIUM", "IT"));
        Transaction tx = deposit("acc-1", "10");
        transactions.pipeInput("acc-1", tx);

        var kv = enriched.readKeyValue();
        assertThat(kv.key).isEqualTo("acc-1");
        assertThat(kv.value.transaction().transactionId()).isEqualTo(tx.transactionId());
        assertThat(kv.value.customerName()).isEqualTo("Ada");
        assertThat(kv.value.segment()).isEqualTo("PREMIUM");
        assertThat(kv.value.country()).isEqualTo("IT");
    }

    @Test
    void unknownCustomerIsStillPublished() {
        transactions.pipeInput("acc-9", deposit("acc-9", "10"));

        EnrichedTransaction out = enriched.readValue();
        assertThat(out.segment()).isEqualTo(EnrichedTransaction.UNKNOWN_SEGMENT);
        assertThat(out.customerName()).isNull();
        assertThat(out.country()).isNull();
    }

    @Test
    void customerUpdateAffectsOnlyLaterTransactions() {
        customers.pipeInput("acc-1", new Customer("acc-1", "Ada", "RETAIL", "IT"));
        transactions.pipeInput("acc-1", deposit("acc-1", "1"));
        customers.pipeInput("acc-1", new Customer("acc-1", "Ada", "PREMIUM", "CH"));
        transactions.pipeInput("acc-1", deposit("acc-1", "2"));

        assertThat(enriched.readValuesToList())
                .extracting(EnrichedTransaction::segment, EnrichedTransaction::country)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("RETAIL", "IT"),
                        org.assertj.core.groups.Tuple.tuple("PREMIUM", "CH"));
    }

    @Test
    void tombstoneDeletesCustomer() {
        customers.pipeInput("acc-1", new Customer("acc-1", "Ada", "PREMIUM", "IT"));
        customers.pipeInput("acc-1", (Customer) null);
        transactions.pipeInput("acc-1", deposit("acc-1", "10"));

        assertThat(enriched.readValue().segment()).isEqualTo(EnrichedTransaction.UNKNOWN_SEGMENT);
    }

    @Test
    void nullTransactionsAreDropped() {
        transactions.pipeInput("acc-1", (Transaction) null);

        assertThat(enriched.isEmpty()).isTrue();
    }
}
