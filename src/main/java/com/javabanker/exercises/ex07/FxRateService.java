package com.javabanker.exercises.ex07;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Reference solution: same behaviour, every lookup goes through the cached FxRateLookup bean. */
public class FxRateService {

    private final FxRateLookup lookup;

    public FxRateService(FxRateLookup lookup) {
        this.lookup = lookup;
    }

    public Optional<BigDecimal> rateToEur(String currency) {
        return lookup.rateToEur(currency);
    }

    public BigDecimal convertToEur(BigDecimal amount, String currency) {
        return rateToEur(currency)
                .map(rate -> amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN))
                .orElseThrow(() -> new IllegalArgumentException("Unknown currency " + currency));
    }

    public void refresh(String currency) {
        lookup.evict(currency);
    }
}
