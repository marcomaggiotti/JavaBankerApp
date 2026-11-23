package com.javabanker.exercises.ex07;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The cached call lives in its own bean. @Cacheable works through a Spring proxy, and a call from
 * inside the same object ({@code this.rateToEur(..)}) bypasses the proxy: the self-invocation trap.
 */
public class FxRateLookup {

    private final FxRateClient client;

    public FxRateLookup(FxRateClient client) {
        this.client = client;
    }

    /** Optional is unwrapped by Spring Cache, so #result is the rate: unknown currencies are not cached. */
    @Cacheable(cacheNames = CachingConfig.FX_RATES, unless = "#result == null")
    public Optional<BigDecimal> rateToEur(String currency) {
        return Optional.ofNullable(client.fetchRateToEur(currency));
    }

    @CacheEvict(cacheNames = CachingConfig.FX_RATES)
    public void evict(String currency) {
        // the annotation does the work
    }
}
