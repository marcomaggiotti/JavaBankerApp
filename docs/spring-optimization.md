# Spring Boot optimization: techniques, examples and challenges

Rule zero: **measure, change one thing, measure again.** Every section below says how to measure the
effect. Three of the techniques are hands-on challenges with tests:
[ex07 caching](../exercises/ex07-caching/README.md), [ex08 parallel calls](../exercises/ex08-parallel-calls/README.md)
and [ex09 N+1](../exercises/ex09-n-plus-one/README.md).

## Applied in this repository

| Technique | Where | Effect |
|---|---|---|
| Class Data Sharing (CDS) archive | `Dockerfile` | startup ~3.5 s → ~2.3 s (measured locally, see §1) |
| Virtual threads for request handling | `application.yml` `spring.threads.virtual.enabled` | blocking calls (Kafka ack, LLM) don't hold platform threads |
| Producer batching + lz4 compression | `application.yml` `spring.kafka.producer.*` | fewer, smaller requests to the broker |
| HTTP response compression | `application.yml` `server.compression.*` | smaller JSON payloads over the wire |
| Container-aware heap | `Dockerfile` `-XX:MaxRAMPercentage=75` | heap follows the pod memory limit |
| Validation before expensive calls | `AskAssistantService` | invalid questions never reach the LLM |
| Local state instead of a DB round trip | `KafkaStreamsBalanceRepository` | balance reads are in-process RocksDB lookups |
| No repartition on the hot path | `BalanceTopology` (input keyed by accountId) | no extra topic hop per transaction |

---

## 0. Measure first

| Question | Tool |
|---|---|
| Which endpoint is slow? p50/p95/p99? | Actuator `/actuator/metrics/http.server.requests`, Prometheus `/actuator/prometheus` |
| Where does the time go inside a request? | Micrometer Observation / tracing (`@Observed`), Java Flight Recorder, async-profiler |
| How does it behave under load? | k6, Gatling or JMeter against `docker compose up` |
| Is this method faster than that one? | JMH microbenchmarks. Never time with a single `System.nanoTime()` call |
| Is the JVM healthy? | `jvm.gc.pause`, `jvm.memory.used`, `jvm.threads.live` metrics, GC logs |

```java
@Observed(name = "statement.build")           // timer + trace span, needs ObservedAspect bean
public List<StatementLine> statement(String accountId) { ... }
```

---

## 1. Startup time

**CDS (Class Data Sharing)** stores parsed classes in an archive the JVM maps at startup. Spring Boot 3.3+
can run a "training" start that stops right after the context refresh, before Kafka or Tomcat start:

```dockerfile
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar banker-platform.jar
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "banker-platform.jar"]
```

Measured in this repo (same machine, two runs each): **3.4–3.7 s without, 2.1–2.5 s with the archive.**
The jar must be run from the extracted layout (`java -Djarmode=tools -jar app.jar extract`), and the archive
must be created with the same JVM that runs it.

| Option | Gain | Cost |
|---|---|---|
| CDS archive | ~30–40 % faster start | bigger image (~60 MB), build-time training run |
| Spring AOT (`spring-boot:process-aot`, `-Dspring.aot.enabled=true`) | less reflection at startup | fixed bean graph at build time (`@Profile` and `@Conditional` are resolved early) |
| GraalVM native image | ~0.1 s start, low memory | long builds, reflection config, no JIT peak performance |
| `spring.main.lazy-initialization=true` | faster start | first request is slow, and config errors surface late. Fine for tests, risky in production |
| Remove unused starters / autoconfig | less to scan and create | ongoing discipline |

---

## 2. Threads and concurrency

**Virtual threads (Java 21)**: one cheap thread per request. When it blocks on I/O it unmounts from its carrier thread.

```yaml
spring.threads.virtual.enabled: true   # Tomcat, @Async, @Scheduled, Kafka listener containers
```

Watch out for:
- **Pinning**: before JDK 24, blocking inside `synchronized` pins the carrier. Prefer `ReentrantLock` in hot paths.
- **Pool-sized resources still limit throughput**: 10 000 virtual threads still share 10 DB connections.
- Don't pool virtual threads, and don't use them for CPU-bound work (no gain).

**Independent remote calls in parallel** ([ex08](../exercises/ex08-parallel-calls/README.md)):

```java
// before: ~900 ms (300 + 300 + 300)
var name = profiles.customerName(id);
var balance = balances.balance(id);
var cards = cardsClient.cardNumbers(id);

// after: ~300 ms, optional call with timeout + fallback
var name = supplyAsync(() -> profiles.customerName(id), executor);
var balance = supplyAsync(() -> balances.balance(id), executor);
var cards = supplyAsync(() -> cardsClient.cardNumbers(id), executor)
        .completeOnTimeout(List.of(), 500, MILLISECONDS)
        .exceptionally(e -> List.of());
return new AccountOverview(name.join(), balance.join(), cards.join());
```

Never run blocking I/O on `ForkJoinPool.commonPool()`. It's shared JVM-wide and sized for CPU work.

---

## 3. Caching

([ex07](../exercises/ex07-caching/README.md))

```java
@Bean
CacheManager cacheManager() {
    var manager = new CaffeineCacheManager("fx-rates");
    manager.setCaffeine(Caffeine.newBuilder().maximumSize(1_000).expireAfterWrite(Duration.ofMinutes(10)).recordStats());
    return manager;
}

@Cacheable(cacheNames = "fx-rates", unless = "#result == null")
public Optional<BigDecimal> rateToEur(String currency) { ... }
```

- **The self-invocation trap**: `@Cacheable`, `@Transactional` and `@Async` work through proxies, so
  `this.method()` skips them. Put the annotated method in another bean.
- Always bound the size and set a TTL. An unbounded cache is a memory leak with a nice name.
- Stampede protection: `@Cacheable(sync = true)` or Caffeine `AsyncCache`.
- Over HTTP, `ETag` / `Cache-Control` let clients and CDNs skip whole requests (`ShallowEtagHeaderFilter`).
- Measure the hit rate: Actuator `cache.gets{result=hit}` vs `{result=miss}`.

---

## 4. Data access

**N+1** ([ex09](../exercises/ex09-n-plus-one/README.md)): one query for the list, plus one per element.

```java
// before: 1 + N queries (lazy association touched in a loop)
List<Payment> payments = repo.findByAccountId(id);
payments.forEach(p -> p.getMerchant().getName());

// after (JPA): 1 query
@Query("select p from Payment p join fetch p.merchant where p.accountId = :id")
List<Payment> findWithMerchant(String id);
// or: @EntityGraph(attributePaths = "merchant"), or @BatchSize(size = 100) / hibernate.default_batch_fetch_size
```

| Technique | Why |
|---|---|
| `spring.jpa.open-in-view=false` | stops lazy loading from the view layer, so N+1 problems show up during development |
| DTO projections (`record` + `select new ...`) | load only the columns you need |
| `@Transactional(readOnly = true)` | Hibernate skips dirty checking. Can route to a read replica |
| JDBC batching: `hibernate.jdbc.batch_size=50`, `order_inserts=true` | many inserts in one round trip (no `IDENTITY` ids) |
| Keyset pagination (`where id > :last`) | stays fast on deep pages, unlike `OFFSET` |
| HikariCP pool size ≈ 2 × CPU cores of the DB | more connections ≠ more throughput |
| Indexes and `EXPLAIN ANALYZE` | most "Java is slow" issues are a missing index |

---

## 5. Web layer

- `server.compression.enabled=true` for JSON responses over ~2 KB (applied).
- Reuse the Spring-managed `ObjectMapper`. Building one per request is expensive.
- Paginate large lists. Stream really large exports with `StreamingResponseBody`.
- Validate early (`@Valid`), so bad input never reaches expensive code (see `AskAssistantService`).
- Set timeouts on every outbound client (`RestClient` / `WebClient` / LLM calls). The default is often "forever".

---

## 6. Kafka producer

```yaml
spring.kafka.producer:
  acks: all                # durability (don't trade it away for speed in a bank)
  compression-type: lz4    # cheap CPU, much less network and disk
  batch-size: 65536
  properties:
    linger.ms: 5           # wait up to 5 ms to fill a batch
    enable.idempotence: true
```

Trade-off in `KafkaTransactionPublisher`: it waits for the broker ack (`send().get()`) so that HTTP 202 means
"durably stored". With virtual threads that wait is cheap. For pure throughput, send asynchronously and handle
failures in the callback.

## 7. Kafka Streams

| Setting / practice | Effect |
|---|---|
| `num.stream.threads` (`KAFKA_STREAMS_THREADS`) | parallelism per instance. Total useful parallelism = input partitions |
| `statestore.cache.max.bytes` + `commit.interval.ms` | dedupes updates per key before writing changelog and output. Bigger = fewer writes, more latency |
| `mapValues` instead of `map` / `selectKey` | keeps the key, so no repartition topic |
| `num.standby.replicas=1` | fast failover without a full state restore |
| RocksDB config (`rocksdb.config.setter`) | tune block cache and write buffers for large state |
| EOS v2 | costs some latency per commit. Keep it for money, consider at-least-once for metrics |

## 8. JVM in containers

- `-XX:MaxRAMPercentage=75` instead of a fixed `-Xmx`, so the heap follows the pod limit (applied).
- G1 (the default) suits most services. ZGC (`-XX:+UseZGC`) for very low pause times with large heaps.
- `-XX:+ExitOnOutOfMemoryError` (applied): let Kubernetes restart a broken pod, and add
  `-XX:+HeapDumpOnOutOfMemoryError` with a volume to analyse it.
- Set CPU requests realistically. The JVM sizes GC and compiler threads from the CPUs it sees.

## 9. Logging

- Parameterized logging, `log.debug("x={}", x)`, never string concatenation. Guard expensive arguments.
- No per-request INFO logs on hot paths. Use metrics for counts.
- Async appenders for high-volume logs. JSON logs (`logging.structured.format.console=ecs`, Boot 3.4+) for log platforms.

---

## Interview questions on performance

- **"The service got slow after a release. What do you do?"** Compare metrics before and after (p99, GC, DB
  time), find the slow span in traces, reproduce it with a load test, profile (JFR or async-profiler), fix one
  thing, and verify against the same load test.
- **"How would you make startup faster?"** Measure with `ApplicationStartup` / `/actuator/startup`, then apply
  CDS, AOT or native in order of effort, and remove unused auto-configuration.
- **"When is caching the wrong answer?"** When the data must be exact (balances: our read model already comes
  from local state), when it changes faster than it's read, or when invalidation across instances is unsolved.
- **"Virtual threads: will they make my app faster?"** They improve *throughput* for I/O-bound,
  thread-per-request code. They don't improve CPU-bound work or the latency of a single request, and pinned
  or pool-limited resources stay bottlenecks.
