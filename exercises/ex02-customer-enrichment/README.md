# Exercise 02 — Customer enrichment ★★ (KStream–KTable join) · 45 min

## Task
The compliance team needs every transaction enriched with customer data.

| Topic | Key | Value |
|---|---|---|
| `bank.transactions.v1` (input) | accountId | `Transaction` |
| `bank.customers.v1` (input, compacted) | accountId | `Customer(accountId, name, segment, country)` |
| `bank.transactions.enriched.v1` (output) | accountId | `EnrichedTransaction(transaction, customerName, segment, country)` |

Implement `CustomerEnrichmentTopology.contribute(...)`.

## Acceptance criteria
1. A transaction for a known customer is enriched with that customer's name, segment and country.
2. A transaction for an unknown customer is still published: `customerName` and `country` are `null`,
   and `segment` is `"UNKNOWN"`.
3. A customer update only affects transactions processed **after** it. Earlier output is not changed.
4. A tombstone (`null` value) on the customers topic deletes the customer, so later transactions are `UNKNOWN`.
5. `null` transactions are dropped.

## Run
```bash
scripts/exercises.sh test ex02
```

<details><summary>Hints</summary>

- `builder.table(topic, Consumed.with(...))` turns a compacted topic into a `KTable`.
- `stream.leftJoin(table, valueJoiner, Joined.with(keySerde, streamValueSerde, tableValueSerde))`.
- A stream–table join is **not** windowed: each stream record looks up the table's *current* value.
</details>

## Interview follow-ups
- What is **co-partitioning**, and why must both topics have the same partition count and partitioner?
- What would change if `bank.customers.v1` were keyed by `customerId`? (A foreign-key join, `KTable.join(other, fkExtractor, joiner)`,
  or re-key the stream.)
- When would you use a `GlobalKTable` instead? Weigh size, bootstrapping and co-partitioning.
- What happens when a transaction arrives *before* the customer record? How could versioned stores help?
