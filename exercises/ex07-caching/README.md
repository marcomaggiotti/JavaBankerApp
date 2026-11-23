# Exercise 07 — Caching a slow remote call ★★ (Spring Cache, Caffeine, proxies) · 45 min

**Optimization challenge:** the starter code is correct but slow. Make it fast **without changing behaviour**.

## Task
`FxRateService` asks a remote FX service for every lookup. Each call takes ~200 ms, is rate-limited and billed.
Profiling shows the same 5 currencies requested thousands of times per minute.

Change `CachingConfig` and the code it wires so that:

## Acceptance criteria
1. Repeated `rateToEur("USD")` calls reach the remote client **once**.
2. `convertToEur(...)` benefits from the cache too. 50 conversions mean 1 remote call.
3. Each currency is cached separately.
4. Unknown currencies (the client returns `null`) are **not** cached: asking again retries the remote service.
5. `refresh(currency)` evicts only that currency. The next lookup calls the client again.
6. The cache is bounded (maximum size) and entries expire (TTL). Behaviour (rounding, exceptions) is unchanged.

## Run
```bash
scripts/exercises.sh test ex07
```

<details><summary>Hints</summary>

- `spring-boot-starter-cache` and Caffeine are already on the classpath. Use `@EnableCaching` with a `CaffeineCacheManager`.
- `@Cacheable(cacheNames = ..., unless = "#result == null")` and `@CacheEvict`.
- Criterion 2 is the trap. `@Cacheable` works through a **proxy**, so a call to `this.rateToEur(...)` inside the
  same class never reaches it. Move the cached method into a separate bean.
- Spring Cache unwraps `Optional`: in `unless`, `#result` is the value inside the Optional.
</details>

## Interview follow-ups
- Where else does the self-invocation trap bite? `@Transactional`, `@Async`, `@Retryable`. How do you fix it?
  Use a separate bean, self-injection with `@Lazy`, or AspectJ weaving.
- Local cache vs distributed cache (Redis): what changes with 10 instances (consistency, invalidation, network hop)?
- Cache stampede: 100 threads miss the same key at once. Caffeine's `AsyncCache`, or `@Cacheable(sync = true)`.
- How do you prove it helped? Look at Actuator's `cache.gets{result=hit|miss}` metrics, before and after latency, and the remote call count.
