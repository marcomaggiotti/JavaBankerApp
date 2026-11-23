package com.javabanker.exercises.ex07;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Reference solution: Spring Cache abstraction backed by Caffeine (bounded size + TTL). */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CachingConfig {

    public static final String FX_RATES = "fx-rates";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(FX_RATES);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)                       // bounded: a cache must never be a memory leak
                .expireAfterWrite(Duration.ofMinutes(10)) // rates change: stale data has a maximum age
                .recordStats());                          // exposed as cache.* metrics by Actuator
        return manager;
    }

    @Bean
    FxRateLookup fxRateLookup(FxRateClient client) {
        return new FxRateLookup(client);
    }

    @Bean
    FxRateService fxRateService(FxRateLookup lookup) {
        return new FxRateService(lookup);
    }
}
