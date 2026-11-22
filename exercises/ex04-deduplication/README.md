# Exercise 04 — Idempotent processing / deduplication ★★★ (Processor API, window store) · 60 min

## Task
Mobile clients retry on timeouts, so the same transaction (same `transactionId`) can arrive several times.
Forward only the **first** occurrence of each `transactionId` seen within the last `window`
(10 minutes in production), measured in **event time** (the record timestamp).

| Topic | Key | Value |
|---|---|---|
| `bank.transactions.v1` (input) | accountId | `Transaction` |
| `bank.transactions.deduplicated.v1` (output) | accountId | `Transaction` (unchanged) |

Implement `DeduplicationTopology` and `DeduplicationProcessor`. Keep the constructor
`DeduplicationTopology(Duration window)` and the topic constants.

## Acceptance criteria
1. The first occurrence of a `transactionId` is forwarded unchanged (same key and value).
2. Another occurrence within `window` of an earlier one is dropped.
3. An occurrence more than `window` after the last forwarded one is forwarded again (state expires).
4. Different `transactionId`s are independent, even on the same account.
5. `null` values are dropped.
6. State must not grow forever: entries older than the window are cleaned up automatically.

## Run
```bash
scripts/exercises.sh test ex04
```

<details><summary>Hints</summary>

- `builder.addStateStore(Stores.windowStoreBuilder(Stores.persistentWindowStore(name, retention, windowSize, false), keySerde, valueSerde))`
- `stream.process(() -> new DeduplicationProcessor(...), storeName)`: pass the store name so it is connected.
- In the processor: `store.fetch(id, from, to)` returns a `WindowStoreIterator`. Close it with try-with-resources.
  `store.put(id, value, timestamp)`.
- Why keep the key as accountId? Retries of the same transaction carry the same accountId, so they land on
  the same partition and task, and that task's store sees all of them. No repartition is needed.
</details>

## Interview follow-ups
- Doesn't `exactly_once_v2` already solve this? No. EOS stops Kafka-internal duplicates on retry or failover,
  not duplicates created by the **client** sending twice.
- Why a window store and not a `KeyValueStore` plus a punctuator for cleanup? Compare segment-based expiry
  with scanning the store.
- Out-of-order duplicates: what if the retry has an *earlier* timestamp? Fetch `[ts - window, ts + window]`.
- Where else would you put the idempotency key? An `Idempotency-Key` HTTP header, a DB unique constraint, the outbox pattern.
