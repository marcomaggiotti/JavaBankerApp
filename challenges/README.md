# Practice challenges

Each challenge follows the same loop: `scripts/new-challenge.sh <Name>` → write tests from the acceptance
criteria → implement → discuss follow-ups. Input is always `bank.transactions.v1`
(key = accountId, value = `Transaction` JSON) unless stated. Difficulty: ★ warm-up … ★★★★ senior.
🏋️ = solved as an exercise (solution, tests, plus starter code to practise) in [../exercises](../exercises/README.md).

| # | Challenge | Concepts | Status |
|---|---|---|---|
| 1 | Account balances with overdraft protection ★★★ | Processor API, state store, branching, IQ | ✅ `streams/topology/BalanceTopology` |
| 2 | Withdrawal velocity fraud check ★★★ | tumbling windows, grace, suppress | ✅ `streams/topology/FraudDetectionTopology` |
| 3 | Large-transaction alert ★ | stateless filter, Named processors | ✅ `challenges/largetransactionalert` |
| 4 | Multi-currency normalisation ★★ | GlobalKTable join | 🏋️ [exercise ex03](../exercises/) |
| 5 | Daily account statement ★★★ | daily windows, aggregate, suppress, time zones | 🏋️ [exercise ex05](../exercises/) |
| 6 | Idempotent processing / dedup ★★★ | window store as TTL cache, transform | 🏋️ [exercise ex04](../exercises/) |
| 7 | Transfers between accounts ★★★★ | flatMap, repartition, consistency | ⬜ |
| 8 | Customer enrichment ★★ | KStream–KTable join, co-partitioning | 🏋️ [exercise ex02](../exercises/) |
| 9 | Dormant account detection ★★★★ | punctuators, wall-clock vs stream time | 🏋️ [exercise ex06](../exercises/) |
| 10 | Card session analytics ★★★ | session windows, merging | ⬜ |

---

## 1. Account balances with overdraft protection (solved)
Maintain the balance per account. A withdrawal that would make the balance negative is **rejected** and
published to `bank.transactions.rejected.v1` with a reason; the balance stays unchanged. Expose the
balance over HTTP.
**Acceptance:** running balance correct per account; overdraft rejected; exact-balance withdrawal allowed;
currency mismatch rejected. **Follow-ups:** Why not `aggregate()`? How do you query across 3 instances?
What happens on rebalance? How do you rebuild state after a bug fix (reset tool, new application.id)?

## 2. Withdrawal velocity fraud check (solved)
Emit **one** `FraudAlert` per account and 1-minute tumbling window when ≥ 3 withdrawals occur.
Accept events up to 10 s late.
**Follow-ups:** tumbling vs hopping vs sliding windows (sliding = "any 60 s span"); `suppress` vs
`emitStrategy(onWindowClose())`; memory bound of `BufferConfig.unbounded()`; real-time alert (emit on
threshold crossing) vs final result.

## 3. Large-transaction alert (solved, warm-up)
Forward every transaction with amount ≥ 10 000 to `bank.large-transactions.v1`.
**Follow-ups:** make the threshold configurable per country (→ join), multi-currency (→ #4).

## 4. Multi-currency normalisation
Topic `bank.fx-rates.v1` (key = currency, value = rate to EUR, compacted). Output every transaction with
`amountEur`. Unknown currency → `bank.transactions.unpriced.v1`.
**Acceptance:** latest rate is used; rate update affects only later transactions; unknown currency routed.
**Hints:** `builder.globalTable(...)`, `stream.leftJoin(globalTable, (k, tx) -> tx.currency(), joiner)`.
**Follow-ups:** GlobalKTable vs KTable (size, bootstrapping, no co-partitioning, no time sync); BigDecimal rounding.

## 5. Daily account statement
For each account and calendar day (Europe/Rome!), emit once: total deposits, total withdrawals,
count, closing net. **Hints:** `TimeWindows.ofSizeAndGrace(Duration.ofDays(1), ...)` is aligned to UTC
epoch — discuss shifting timestamps or a custom processor for local days; `aggregate` into a
`DailyStatement` record; `suppress`.
**Follow-ups:** DST days are 23/25 h; retention of window stores (`Materialized.withRetention`).

## 6. Idempotent processing / deduplication
Clients may retry: the same `transactionId` can arrive multiple times within 10 minutes. Forward only the
first occurrence. **Hints:** `process()` with a `WindowStore<String, Long>` keyed by transactionId used as a
TTL cache (retention 10 min); key must be the accountId to stay co-partitioned.
**Follow-ups:** EOS guarantees vs business-level idempotency; idempotency key in the REST API header.

## 7. Transfers between accounts
New event `Transfer(from, to, amount)`. Produce a debit on `from` and a credit on `to`; reject if
insufficient funds. **Hints:** first validate & debit in the `from` partition, then `flatMap` to a credit
event keyed by `to` → automatic repartition. Discuss the saga: what if the credit side fails? Compensation.
**Follow-ups:** why you can't do a distributed atomic update across partitions; outbox pattern.

## 8. Customer enrichment
`bank.customers.v1` (key = accountId, compacted: name, segment, country). Enrich each transaction.
**Hints:** `builder.table(...)`, `stream.join(table, joiner)` — both topics must have the **same number of
partitions and partitioner** (co-partitioning). **Follow-ups:** what if customers topic is keyed by
customerId? (foreign-key join `KTable.join(other, fkExtractor, ...)`).

## 9. Dormant account detection
Emit `AccountDormant` if an account has no transaction for 30 days (test with 30 s).
**Hints:** store last-seen timestamp per account; `context.schedule(interval, PunctuationType.WALL_CLOCK_TIME, ...)`
scans the store. In the test use `driver.advanceWallClockTime(...)`.
**Follow-ups:** stream-time vs wall-clock punctuation; cost of full store scans; emit only once.

## 10. Card session analytics
Group card withdrawals into sessions separated by ≥ 5 minutes of inactivity; emit count & total per session.
**Hints:** `SessionWindows.ofInactivityGapAndGrace(...)`, `aggregate(init, agg, merger)`.
**Follow-ups:** why sessions need a *merger*; tombstones emitted when sessions merge.

---

### Bonus (cross-cutting) tasks often asked in take-homes
- Add a DLQ for poison pills (custom `DeserializationExceptionHandler`).
- Replace JSON with Avro + Schema Registry (compose service `confluentinc/cp-schema-registry`).
- Forward interactive queries to the owning instance (`queryMetadataForKey`).
- Add Testcontainers (`org.testcontainers:kafka`) instead of `@EmbeddedKafka`.
- Port one topology to **Apache Flink** (DataStream API, `KeyedProcessFunction`) and compare.
- Rewrite a challenge in **Kotlin** (add `kotlin-maven-plugin`; topologies read very nicely with lambdas).
