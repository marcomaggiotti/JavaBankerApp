package com.javabanker.exercises.ex07;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Exercise 07 - works, but calls the remote service on EVERY lookup.
 * Make it fast without changing behaviour: see exercises/ex07-caching/README.md
 */
public class FxRateService {

    private final FxRateClient client;

    public FxRateService(FxRateClient client) {
        this.client = client;
    }

    public Optional<BigDecimal> rateToEur(String currency) {
        return Optional.ofNullable(client.fetchRateToEur(currency));
    }

    public BigDecimal convertToEur(BigDecimal amount, String currency) {
        return rateToEur(currency)
                .map(rate -> amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN))
                .orElseThrow(() -> new IllegalArgumentException("Unknown currency " + currency));
    }

    /** Forget the cached rate of one currency (e.g. when the FX desk publishes a correction). */
    public void refresh(String currency) {
        // nothing to refresh: there is no cache yet
    }
}
