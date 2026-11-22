# Exercise 09 — The N+1 problem ★★ (bulk loading, chunking) · 30 min

**Optimization challenge:** the starter code is correct but slow. Make it fast **without changing behaviour**.

## Task
`StatementService.statement(accountId)` loads an account's transactions, then calls the remote merchant
directory **once per transaction** to get the merchant name. 500 transactions means 501 round trips of ~50 ms:
the classic **N+1 query problem**. It's the same thing JPA does with lazy associations.

## Acceptance criteria
1. Same output: one `StatementLine` per transaction, in the original order, with merchant name and category.
2. Unknown merchants get `"UNKNOWN"` as name and category.
3. `findById` is never called. Merchants are loaded with `findAllByIds`, each distinct id requested once.
4. The bulk endpoint accepts at most `MerchantClient.MAX_IDS_PER_REQUEST` (100) ids per call, so split bigger sets into chunks.
5. An account without transactions makes **no** remote call.

## Run
```bash
scripts/exercises.sh test ex09
```

<details><summary>Hints</summary>

- Collect distinct ids with a `LinkedHashSet`, so you dedupe and keep a stable order.
- Chunk with `subList(from, Math.min(from + 100, size))`, merge the maps, then map rows to lines in memory.
</details>

## Interview follow-ups (JPA edition)
- How do you **detect** N+1? Hibernate statistics, `spring.jpa.properties.hibernate.generate_statistics=true`,
  SQL logging, datasource-proxy, or tests that assert the query count.
- Fixes in JPA: `JOIN FETCH` in JPQL, `@EntityGraph(attributePaths = "merchant")`, `@BatchSize(size = 100)`
  or `hibernate.default_batch_fetch_size`, and DTO projections.
- Why not just make every association `EAGER`? Because it loads data on every query, including the ones that
  don't need it, and you get cartesian products.
- `JOIN FETCH` with pagination: why does Hibernate warn `HHH000104` (applying the limit in memory)?
