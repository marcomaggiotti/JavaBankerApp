# Exercise 08 — Parallelise independent remote calls ★★★ (CompletableFuture, virtual threads, timeouts) · 45 min

**Optimization challenge:** the starter code is correct but slow. Make it fast **without changing behaviour**.

## Task
`GET /accounts/{id}/overview` calls three independent services, each taking ~300 ms: profile, balance and cards.
The starter calls them one after another (~900 ms), and if the cards service hangs, the whole page hangs.

Rewrite `AccountOverviewService.overview(accountId)`. Keep the constructor: the `Executor` is injected, and
the tests pass `Executors.newVirtualThreadPerTaskExecutor()`.

## Acceptance criteria
1. Same result as before: `AccountOverview(customerName, balance, cards)`.
2. The three calls run **concurrently**: total latency is about the slowest call, not the sum.
3. Cards are optional. If the cards service fails, `cards` is an empty list.
4. If the cards service is slower than `cardsTimeout`, answer anyway with empty `cards`. Don't wait for it.
5. Profile and balance are mandatory. Their failure propagates **with the original exception type**
   (e.g. `AccountNotFoundException`, not a `CompletionException`).

## Run
```bash
scripts/exercises.sh test ex08
```

<details><summary>Hints</summary>

- `CompletableFuture.supplyAsync(supplier, executor)` for each call, then `join()` them all.
- `completeOnTimeout(fallback, timeout, unit)` and `exceptionally(e -> fallback)` for the optional call.
- `join()` wraps failures in `CompletionException`, so unwrap `getCause()` before rethrowing.
- Java 21 alternative: structured concurrency (`StructuredTaskScope`, preview in 21) expresses this even more cleanly.
</details>

## Interview follow-ups
- Why inject the `Executor` instead of using `ForkJoinPool.commonPool()`? The common pool is sized for CPU-bound
  work and shared JVM-wide, so blocking I/O there starves everything else.
- Virtual threads (`spring.threads.virtual.enabled=true`): what changes for blocking code? Watch for pinning on
  `synchronized` (before JDK 24) and for pool-sized resources such as DB connections.
- What about thread-locals (MDC, security context, tracing) across threads? Spring's `TaskDecorator` and context propagation.
- If the cards service times out a lot, what's next? A circuit breaker (Resilience4j), a cache, or a bulkhead.
