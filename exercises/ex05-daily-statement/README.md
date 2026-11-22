# Exercise 05 — Daily account statement ★★★ (windowed aggregation, grace, suppress) · 60 min

## Task
At the end of each **UTC** calendar day, publish **one** statement per account that had activity that day.

| Topic | Key | Value |
|---|---|---|
| `bank.transactions.v1` (input) | accountId | `Transaction` |
| `bank.daily-statements.v1` (output) | accountId | `DailyStatement(accountId, day, totalDeposits, totalWithdrawals, transactionCount, net)` |

Implement `DailyStatementTopology(Duration grace)`. Events can arrive up to `grace` late.

## Acceptance criteria
1. Nothing is emitted while the day is still open. Exactly **one** statement per account and day is emitted
   once the day plus the grace period has passed (in stream time).
2. `net = totalDeposits − totalWithdrawals`, and `transactionCount` counts both kinds.
3. A late event that arrives within the grace period is included.
4. A late event that arrives after the window closed is dropped, and no second statement is emitted.
5. Different days and accounts produce separate statements. `day` is the UTC date of the window start.

## Run
```bash
scripts/exercises.sh test ex05
```

<details><summary>Hints</summary>

- `groupByKey(Grouped.with(...)).windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofDays(1), grace))`
- `.aggregate(initializer, aggregator, Materialized.<K, AGG, WindowStore<Bytes, byte[]>>as(...).withKeySerde(..).withValueSerde(..))`.
  You will probably want your own small aggregate record (it must be JSON-serialisable).
- `.suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))` gives the *final* results only.
- Windows are aligned to the epoch, so 1-day windows start at 00:00 **UTC**.
- In tests, windows close only when **stream time** moves forward, so pipe a later record (a "clock tick").
</details>

## Interview follow-ups
- The bank is in Europe/Rome. How do you produce *local* days? DST days have 23 or 25 hours.
  Options: a custom `TimestampExtractor`, or a Processor API solution keyed by `(accountId, localDate)`.
- What does `BufferConfig.unbounded()` risk? Use `maxRecords` or `maxBytes` with `shutDownWhenFull` or `emitEarlyWhenFull`.
- Compare with `emitStrategy(EmitStrategy.onWindowClose())`.
- Window store retention: `Materialized.withRetention(...)` must be at least window size + grace.
