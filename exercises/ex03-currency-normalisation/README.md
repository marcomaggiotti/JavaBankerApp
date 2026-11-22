# Exercise 03 — Currency normalisation ★★ (GlobalKTable join, branching) · 45 min

## Task
Reporting works in EUR. Convert every transaction to EUR using the latest exchange rate.

| Topic | Key | Value |
|---|---|---|
| `bank.transactions.v1` (input) | accountId | `Transaction` (any ISO currency) |
| `bank.fx-rates.v1` (input, compacted) | currency | `FxRate(currency, rateToEur)` |
| `bank.transactions.eur.v1` (output) | accountId | `NormalisedTransaction(transaction, amountEur)` |
| `bank.transactions.unpriced.v1` (output) | accountId | `Transaction` (original) |

Implement `CurrencyNormalisationTopology.contribute(...)`.

## Acceptance criteria
1. `amountEur = amount × rateToEur`, rounded to 2 decimals with `HALF_EVEN`.
2. EUR transactions don't need a rate: `amountEur = amount`.
3. The **latest** rate at processing time is used. A rate update doesn't change earlier output.
4. A currency with no known rate goes **unchanged** to `bank.transactions.unpriced.v1`, never to the EUR topic.
5. The output key stays the accountId.

## Run
```bash
scripts/exercises.sh test ex03
```

<details><summary>Hints</summary>

- `builder.globalTable(topic, Consumed.with(...))`
- `stream.leftJoin(globalTable, (key, value) -> lookupKey, (value, tableValue) -> result)`: the second
  argument picks the table key from the record, so the stream does **not** need to be re-keyed.
- Route the two cases with `split().branch(...).defaultBranch(...)`.
</details>

## Interview follow-ups
- GlobalKTable vs KTable: every instance holds the whole table, it is fully loaded before processing starts,
  there is no time synchronisation, and no co-partitioning is needed.
- What if the rate arrives a few milliseconds after the transaction? Options: retry the unpriced topic,
  buffer with a state store, or use a stream–stream join.
- Why `BigDecimal` with an explicit `RoundingMode`? What does `HALF_EVEN` (banker's rounding) avoid?
