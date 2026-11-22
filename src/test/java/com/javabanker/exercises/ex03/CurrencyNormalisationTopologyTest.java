package com.javabanker.exercises.ex03;

import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionType;
import com.javabanker.support.TopologyTestSupport;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("exercise")
class CurrencyNormalisationTopologyTest extends TopologyTestSupport {

    private TestInputTopic<String, Transaction> transactions;
    private TestInputTopic<String, FxRate> rates;
    private TestOutputTopic<String, NormalisedTransaction> normalised;
    private TestOutputTopic<String, Transaction> unpriced;

    @BeforeEach
    void setUp() {
        start(new CurrencyNormalisationTopology());
        transactions = input(CurrencyNormalisationTopology.TRANSACTIONS, Transaction.class);
        rates = input(CurrencyNormalisationTopology.FX_RATES, FxRate.class);
        normalised = output(CurrencyNormalisationTopology.NORMALISED, NormalisedTransaction.class);
        unpriced = output(CurrencyNormalisationTopology.UNPRICED, Transaction.class);
    }

    @Test
    void convertsWithLatestRateAndRounding() {
        rates.pipeInput("USD", new FxRate("USD", new BigDecimal("0.9")));
        transactions.pipeInput("acc-1", tx("acc-1", "100", "USD"));
        rates.pipeInput("USD", new FxRate("USD", new BigDecimal("0.333333")));
        transactions.pipeInput("acc-1", tx("acc-1", "10", "USD"));

        var out = normalised.readKeyValuesToList();
        assertThat(out).hasSize(2);
        assertThat(out.get(0).key).isEqualTo("acc-1");
        assertThat(out.get(0).value.amountEur()).isEqualByComparingTo("90.00");
        assertThat(out.get(1).value.amountEur()).isEqualByComparingTo("3.33");
        assertThat(out.get(1).value.amountEur().scale()).isEqualTo(2);
    }

    @Test
    void eurNeedsNoRate() {
        transactions.pipeInput("acc-1", tx("acc-1", "12.5", "EUR"));

        assertThat(normalised.readValue().amountEur()).isEqualByComparingTo("12.50");
        assertThat(unpriced.isEmpty()).isTrue();
    }

    @Test
    void unknownCurrencyGoesToUnpricedUnchanged() {
        Transaction gbp = tx("acc-1", "5", "GBP");
        transactions.pipeInput("acc-1", gbp);

        assertThat(normalised.isEmpty()).isTrue();
        assertThat(unpriced.readValue()).isEqualTo(gbp);
    }

    private static Transaction tx(String accountId, String amount, String currency) {
        return new Transaction(UUID.randomUUID().toString(), accountId, TransactionType.DEPOSIT,
                new BigDecimal(amount), currency, T0);
    }
}
