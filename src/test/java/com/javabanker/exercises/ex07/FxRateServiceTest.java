package com.javabanker.exercises.ex07;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("exercise")
@SpringJUnitConfig({CachingConfig.class, FxRateServiceTest.FakeRemote.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // fresh cache per test
class FxRateServiceTest {

    @Autowired
    FxRateService service;

    @Autowired
    CountingFxRateClient remote;

    @Test
    void repeatedLookupsHitTheRemoteServiceOnce() {
        for (int i = 0; i < 100; i++) {
            assertThat(service.rateToEur("USD")).hasValueSatisfying(r -> assertThat(r).isEqualByComparingTo("0.9"));
        }
        assertThat(remote.calls("USD")).isEqualTo(1);
    }

    @Test
    void conversionsUseTheCacheToo() {
        for (int i = 0; i < 50; i++) {
            assertThat(service.convertToEur(new BigDecimal("10"), "USD")).isEqualByComparingTo("9.00");
        }
        assertThat(remote.calls("USD")).as("self-invocation must not bypass the cache").isEqualTo(1);
    }

    @Test
    void currenciesAreCachedSeparately() {
        service.rateToEur("USD");
        service.rateToEur("CHF");
        service.rateToEur("USD");
        service.rateToEur("CHF");

        assertThat(remote.calls("USD")).isEqualTo(1);
        assertThat(remote.calls("CHF")).isEqualTo(1);
    }

    @Test
    void unknownCurrenciesAreNotCached() {
        for (int i = 0; i < 3; i++) {
            assertThat(service.rateToEur("XYZ")).isEmpty();
        }
        assertThat(remote.calls("XYZ")).isEqualTo(3);
        assertThatThrownBy(() -> service.convertToEur(BigDecimal.ONE, "XYZ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refreshEvictsOnlyThatCurrency() {
        service.rateToEur("USD");
        service.rateToEur("CHF");
        service.refresh("USD");
        service.rateToEur("USD");
        service.rateToEur("CHF");

        assertThat(remote.calls("USD")).isEqualTo(2);
        assertThat(remote.calls("CHF")).isEqualTo(1);
    }

    @Configuration(proxyBeanMethods = false)
    static class FakeRemote {
        @Bean
        CountingFxRateClient fxRateClient() {
            return new CountingFxRateClient();
        }
    }

    static class CountingFxRateClient implements FxRateClient {
        private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();

        @Override
        public BigDecimal fetchRateToEur(String currency) {
            calls.computeIfAbsent(currency, c -> new AtomicInteger()).incrementAndGet();
            return switch (currency) {
                case "USD" -> new BigDecimal("0.9");
                case "CHF" -> new BigDecimal("1.05");
                default -> null;
            };
        }

        int calls(String currency) {
            return calls.getOrDefault(currency, new AtomicInteger()).get();
        }
    }
}
