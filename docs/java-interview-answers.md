# Java interview questions, answered

Short, interview-ready answers with Java code. Answers marked 🧪 are proven by executable tests:
[`CoreJavaFactsTest`](../src/test/java/com/javabanker/interview/CoreJavaFactsTest.java) for language facts and
[`CodingQuestionsTest`](../src/test/java/com/javabanker/interview/CodingQuestionsTest.java) for coding questions,
with solutions in [`src/main/java/com/javabanker/interview`](../src/main/java/com/javabanker/interview).
Run them:

```bash
mvn test -Dtest='CoreJavaFactsTest,CodingQuestionsTest'
```

How to answer in an interview: **definition in one sentence → why it matters → a concrete example → the trade-off.**

---

## 1. Core language

**Q: `==` vs `equals()`?** 🧪
`==` compares references (or primitive values). `equals()` compares logical equality as the class defines it.
Trap: `Integer` values from -128 to 127 are cached, so `==` *seems* to work for small numbers and breaks at 128.
Always use `equals()` for objects.

**Q: What is the `equals`/`hashCode` contract?** 🧪
Equal objects **must** have equal hash codes (the reverse isn't required). If you override `equals` without
`hashCode`, `HashSet`/`HashMap` look in the wrong bucket and "lose" elements. Records generate both correctly.

**Q: Why is `String` immutable?**
Security (class names, URLs and file paths can't change after a check), thread-safety for free, a cacheable
`hashCode`, and the string pool. Use `StringBuilder` for concatenation in loops.

**Q: Is Java pass-by-value or pass-by-reference?** 🧪
Always **pass-by-value**. For objects, the value passed is the reference. Reassigning the parameter doesn't affect
the caller, but mutating the referenced object does.

**Q: Why `BigDecimal` for money?** 🧪
`double` is binary floating point: `0.1 + 0.2 != 0.3`. Use `new BigDecimal("0.1")` (never `new BigDecimal(0.1)`),
an explicit `RoundingMode`, and `compareTo` (because `equals` also compares the scale: `1.0 ≠ 1.00`).

**Q: `final`, `finally`, `finalize`?**
`final` means it can't be reassigned (variable), overridden (method) or extended (class). `finally` always runs
after `try`/`catch`. `finalize` is deprecated for removal: use `try-with-resources` or `Cleaner`.

**Q: Checked vs unchecked exceptions?** 🧪 (try-with-resources)
Checked exceptions (`IOException`) must be declared or caught, and model recoverable conditions the caller must
handle. Unchecked ones (`RuntimeException`) are programming errors or conditions the caller can't sensibly
handle. `try-with-resources` closes resources in reverse order and keeps close failures as *suppressed* exceptions.

**Q: What are generics and type erasure?** 🧪
Compile-time type safety. At runtime the type arguments are erased, so `List<String>` and `List<Integer>` are the
same class. That's why you can't do `new T()` or `instanceof List<String>`. PECS: **P**roducer `extends`,
**C**onsumer `super` (`Collections.copy(List<? super T> dest, List<? extends T> src)`).

**Q: Abstract class vs interface?**
An interface is a capability contract: multiple inheritance of type, `default` methods, no state.
An abstract class is a partial implementation with shared state and constructors (single inheritance).
Prefer interfaces plus composition.

**Q: How do you write an immutable class?**
Make the class `final` (or use a `record`), give it `private final` fields and no setters, defensively copy
mutable inputs and outputs (`List.copyOf`), and validate in the constructor. Example: `domain/Transaction.java`.

---

## 2. Collections

**Q: How does `HashMap` work?**
An array of buckets indexed by `hash(key) & (n-1)`. Collisions form a linked list that becomes a red-black tree
at 8 entries (Java 8+). It resizes ×2 at load factor 0.75. Average O(1), worst case O(log n). It's **not**
thread-safe, and it allows one `null` key.

**Q: What happens if you mutate a key after putting it in a `HashMap`?** 🧪
Its hash changes, so `get` looks in the wrong bucket. The entry is unreachable but still uses memory.
Keys must be immutable.

**Q: `ArrayList` vs `LinkedList`?**
`ArrayList`: O(1) random access and cache-friendly, which makes it the default. `LinkedList`: O(1) insert at a
known node but O(n) access and poor locality. It's rarely the right choice; `ArrayDeque` beats it for queues.

**Q: `ConcurrentModificationException`?** 🧪
Fail-fast iterators detect structural changes made outside the iterator. Fix it with `removeIf`,
`iterator.remove()`, or a concurrent collection.

**Q: `Collections.unmodifiableList` vs `List.of` vs `Arrays.asList`?** 🧪
An unmodifiable list is a read-only *view* that still reflects changes to the source. `List.of` / `List.copyOf`
are truly immutable (and reject nulls). `Arrays.asList` is fixed-size but writable, backed by the array.

**Q: `HashMap` vs `ConcurrentHashMap` vs `Collections.synchronizedMap`?**
`synchronizedMap` uses one lock for everything. `ConcurrentHashMap` locks per bin with CAS, reads don't block,
and it has atomic `compute`/`merge`/`putIfAbsent`. Iteration is weakly consistent, not fail-fast.

---

## 3. Concurrency

**Q: `synchronized` vs `volatile` vs `Atomic*`?** 🧪
`volatile` gives visibility and ordering only, not atomicity (`count++` is still a race).
`synchronized` gives mutual exclusion plus visibility. `AtomicInteger` gives lock-free atomic read-modify-write (CAS).

**Q: What is a happens-before relationship?**
The Java Memory Model's guarantee that one action's writes are visible to another. Examples: unlocking a monitor
happens-before the next lock of it; a `volatile` write happens-before subsequent reads; `Thread.start()`
happens-before the thread's actions.

**Q: How do you avoid deadlocks?**
Acquire locks in a global order, use `tryLock` with a timeout, keep critical sections small, and prefer
higher-level tools (`ConcurrentHashMap`, `BlockingQueue`, immutable data).

**Q: Implement producer-consumer.** 🧪 [`BoundedBuffer`](../src/main/java/com/javabanker/interview/BoundedBuffer.java)
One `ReentrantLock` with two `Condition`s, always `await()` in a `while` loop (spurious wake-ups).
In production: `ArrayBlockingQueue`.

**Q: `ExecutorService`, `CompletableFuture`, virtual threads?** 🧪
Executors decouple task submission from threads. `CompletableFuture` composes asynchronous results
(`thenCombine`, `completeOnTimeout`, `exceptionally`). Virtual threads (Java 21) are cheap JVM-scheduled threads
for blocking I/O: 10 000 sleeping tasks finish in about the time of one. They don't speed up CPU-bound work;
watch for pinning in `synchronized` before JDK 24. See exercise ex08.

**Q: Why not `parallelStream()` everywhere?**
It uses the shared `ForkJoinPool.commonPool()`. It only helps CPU-bound work on large data with cheap splitting,
and it hurts with blocking I/O, small collections or ordered operations.

---

## 4. JVM, memory and GC

**Q: What's the JVM memory layout?**
The **heap** holds objects (young generation: eden and survivors; old generation). Each thread's **stack** holds
frames, locals and references. **Metaspace** holds class metadata (native memory). There are also the code
cache and direct buffers.

**Q: How does garbage collection work? Which collector would you choose?**
It's generational: most objects die young, so the young generation is collected often and cheaply. Reachability
starts from GC roots. **G1** (the default) balances pauses and throughput. **ZGC** gives sub-millisecond pauses
for large heaps. **Parallel** maximises batch throughput.

**Q: Can Java leak memory?**
Yes, through anything that keeps objects reachable: static collections, unbounded caches, listeners never
removed, `ThreadLocal` in thread pools, or mutable `HashMap` keys. Diagnose with a heap dump
(`-XX:+HeapDumpOnOutOfMemoryError`) and Eclipse MAT.

**Q: How does the JVM behave in a container?**
It reads cgroup limits. Size the heap with `-XX:MaxRAMPercentage`, not a fixed `-Xmx`, and leave room for
metaspace, threads and direct memory (see the `Dockerfile`).

**Q: What does JIT do?**
It interprets bytecode first, then compiles hot methods (C1, then C2) with profile-guided optimizations:
inlining, escape analysis, loop unrolling. That's why benchmarks need warm-up (JMH).

---

## 5. Modern Java (8 → 21)

| Version | Feature to mention | Example in repo |
|---|---|---|
| 8 | lambdas, streams, `Optional`, `java.time` | everywhere |
| 10 | `var` | tests |
| 14 | switch expressions | `BankingTools.explain` |
| 15 | text blocks | `SpringAiBankAssistant.SYSTEM_PROMPT`, tests |
| 16 | **records**, `instanceof` pattern | `domain/*` |
| 17 | **sealed** classes (LTS) | `CoreJavaFactsTest.Payment` |
| 21 | **virtual threads**, record patterns, pattern matching for `switch`, sequenced collections (LTS) | `spring.threads.virtual.enabled`, `CoreJavaFactsTest.describe` |

**Q: Are streams lazy?** 🧪 Yes. Intermediate operations run only when a terminal operation pulls elements, and
short-circuiting operations (`findFirst`) stop early.

**Q: `Optional.orElse` vs `orElseGet`?** 🧪 `orElse(x)` always evaluates `x`. `orElseGet(supplier)` evaluates only
when the Optional is empty. Use `orElseGet` for anything expensive. Don't use `Optional` for fields or parameters.

**Q: Why records + sealed interfaces?** 🧪 Together they give algebraic data types. A `switch` over a sealed type
is checked for exhaustiveness by the compiler, so adding a subtype breaks the build instead of production.

---

## 6. OOP, design and clean code

**Q: Explain SOLID with an example from your code.**
- **S**: `AccountBalance` holds the balance rules, while `BalanceProcessor` only moves data between Kafka and the store.
- **O**: a new stream feature is a new `TopologyContributor` bean, with no changes to existing ones.
- **L**: any `BankAssistant` (Spring AI, the disabled fallback, a test fake) is substitutable.
- **I**: small ports: `GetBalanceUseCase` and `SubmitTransactionUseCase` instead of one `BankService`.
- **D**: the application core depends on the `TransactionPublisher` interface. Kafka implements it.

**Q: Composition vs inheritance?** Prefer composition: it's looser coupling, you can change it at runtime, and it
avoids the fragile-base-class problem. Use inheritance only for a genuine *is-a* relationship with a stable base.

**Q: Which design patterns do you use daily?**
Builder (Kafka `Materialized`, `ChatClient.builder`), Strategy (`TopologyContributor`), Adapter (hexagonal adapters),
Factory (`JsonSerdes.of`), Proxy (Spring AOP: `@Transactional`, `@Cacheable`), Template method (`JdbcTemplate`),
Observer (Kafka itself).

**Q: Singleton in Java?** An `enum` with one constant (serialization- and reflection-safe), or the
initialization-on-demand holder idiom. In Spring, beans are singletons by default, so you rarely need the pattern.

**Q: What is Clean Architecture / hexagonal?** The domain and use cases are at the center and have no framework
dependencies. Ports are interfaces; adapters (web, Kafka, AI) are plugged in from outside. Dependencies point
inwards. Here that's enforced by `ArchitectureTest`. See the README, Architecture section.

---

## 7. Spring and Spring Boot

**Q: How does dependency injection work in Spring?** The container creates beans from configuration and
component scanning, resolves dependencies (prefer **constructor injection**: immutable, testable, no hidden
dependencies), and manages their lifecycle and scopes (singleton, prototype, request…).

**Q: How does auto-configuration work?** Starters bring dependencies. `@AutoConfiguration` classes listed in
`META-INF/spring/...AutoConfiguration.imports` are guarded by `@ConditionalOnClass`, `@ConditionalOnMissingBean`
and `@ConditionalOnProperty`. Your own beans win; for example, this app defines its own `KafkaTemplate`.

**Q: Why doesn't my `@Transactional` / `@Cacheable` work?**
These work through proxies, so a call from the same class (`this.method()`) bypasses the proxy. The same goes
for private or final methods. Exercise ex07 is exactly this trap.

**Q: What are the propagation levels of `@Transactional`?**
`REQUIRED` (default: join or create), `REQUIRES_NEW` (suspend and start a new one, e.g. audit logs), `MANDATORY`,
`NESTED` (savepoint). Only unchecked exceptions roll back by default (`rollbackFor` changes that).

**Q: How do you test a Spring Boot app?** Unit tests without Spring first. Then slices (`@WebMvcTest`,
`@DataJpaTest`) with `@MockitoBean`, a few `@SpringBootTest` integration tests (embedded Kafka or
Testcontainers), plus architecture tests (ArchUnit).

**Q: What is Spring AI?** An abstraction over LLM providers: `ChatClient`, prompts, tool calling with `@Tool`,
structured output, RAG and vector stores. The provider (Ollama, Claude, …) is configuration. This repo's
assistant is in `adapter/out/ai`.

**Q: How would you make a Spring Boot service faster?** Measure first. Then: caching, parallel I/O, fixing N+1,
virtual threads, and for startup CDS, AOT or native. See [spring-optimization.md](spring-optimization.md).

---

## 8. Coding questions (solved, tested) 🧪

| Question | Solution | Complexity |
|---|---|---|
| LRU cache with O(1) get and put | [`LruCache`](../src/main/java/com/javabanker/interview/LruCache.java): `LinkedHashMap` in access order + `removeEldestEntry` | O(1) |
| Balanced brackets | [`BalancedBrackets`](../src/main/java/com/javabanker/interview/BalancedBrackets.java): stack (`ArrayDeque`) | O(n) |
| Two amounts that sum to a target | [`TwoSum`](../src/main/java/com/javabanker/interview/TwoSum.java): one pass with `HashMap` | O(n) |
| Top-k most frequent merchants | [`TopMerchants`](../src/main/java/com/javabanker/interview/TopMerchants.java): `groupingBy` + `counting`, sort | O(n + m log m) |
| First non-repeating character | [`FirstUniqueCharacter`](../src/main/java/com/javabanker/interview/FirstUniqueCharacter.java): `LinkedHashMap` counts | O(n) |
| Producer-consumer / blocking queue | [`BoundedBuffer`](../src/main/java/com/javabanker/interview/BoundedBuffer.java): lock + two conditions | O(1) per operation |
| Bank account with statement | [exercise ex01](../exercises/ex01-bank-account-kata/README.md) | |

```java
// LRU cache: the whole trick
new LinkedHashMap<K, V>(16, 0.75f, true) {          // true = access order
    protected boolean removeEldestEntry(Map.Entry<K, V> e) { return size() > capacity; }
};
```

When solving live: state the brute-force approach and its complexity, then optimize, then **test edge cases out
loud** (empty input, duplicates, one element, very large input).

---

## 9. Kafka and Kafka Streams (short answers)

See [study-guide.md](study-guide.md#3-kafka--kafka-streams--weeks-23-core-of-the-role) for the full list.

- **Ordering?** Only within a partition, so key by the entity whose order matters (here: `accountId`).
- **Exactly-once?** Idempotent producer + transactions + `processing.guarantee=exactly_once_v2`. Only inside Kafka.
- **KStream vs KTable?** A stream of facts vs the latest value per key (a changelog). The stream-table duality.
- **How does state survive a crash?** Through changelog topics and standby replicas.
