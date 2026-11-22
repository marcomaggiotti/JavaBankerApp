# Exercise 06 — Dormant account detection ★★★★ (punctuators, wall-clock time) · 75 min

## Task
Regulation requires flagging accounts with no activity for a long time (30 days in production,
seconds in the tests). Publish a `DormantAccountAlert(accountId, lastActivity, detectedAt)` when an account
has had no transaction for `inactivity`.

| Topic | Key | Value |
|---|---|---|
| `bank.transactions.v1` (input) | accountId | `Transaction` |
| `bank.dormant-accounts.v1` (output) | accountId | `DormantAccountAlert` |

Implement `DormantAccountTopology(Duration inactivity, Duration checkInterval)`.

## Acceptance criteria
1. Check every `checkInterval` of **wall-clock** time, even if no new transactions arrive.
2. Alert when `now − lastActivity ≥ inactivity`. `lastActivity` is the timestamp of the account's last record,
   and `detectedAt` is the wall-clock time of the check.
3. Alert **once** per dormant period. No repeated alerts on later checks.
4. New activity resets the account. If it goes quiet again, it is alerted again.
5. Active accounts are never alerted.

## Run
```bash
scripts/exercises.sh test ex06
```

<details><summary>Hints</summary>

- You need state plus timers, so use the Processor API: `stream.process(supplier, storeName)`.
- In `init`: `context.schedule(interval, PunctuationType.WALL_CLOCK_TIME, timestamp -> ...)`.
- Store per account: last activity and whether you already alerted (a small record).
- In the punctuator, iterate with `store.all()` and **close the iterator** (try-with-resources).
  Collect the matches first, then `put` and `forward`.
- Forwarding from a punctuator: `context.forward(new Record<>(key, value, timestamp))`.
- In tests: `driver.advanceWallClockTime(Duration)` triggers wall-clock punctuations.
</details>

## Interview follow-ups
- `STREAM_TIME` vs `WALL_CLOCK_TIME` punctuation: why would stream time never fire for a dormant account?
- This compares event timestamps with the wall clock. When does that go wrong (replays, lagging consumers)?
- `store.all()` scans everything every interval. How would you scale to 50M accounts?
  (A secondary index by `lastActivity`, a range scan on a time-prefixed key, a larger interval.)
- What happens to the schedule after a rebalance or restart? (Re-registered in `init`. The state comes back from the changelog.)
