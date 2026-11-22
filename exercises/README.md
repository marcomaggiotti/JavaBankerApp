# Coding-challenge exercises

Six timed exercises in the style of real interview tasks. Each one has:

| | Where |
|---|---|
| Task statement, acceptance criteria, hints, follow-up questions | `exercises/exNN-*/README.md` |
| Starter code you edit (compiles, throws `TODO`) | `src/main/java/com/javabanker/exercises/exNN/` |
| Tests = the acceptance criteria (red until solved) | `src/test/java/com/javabanker/exercises/exNN/` |
| Reference solution (don't peek too early) | `exercises/exNN-*/solution/` |

| # | Exercise | Skills | Time |
|---|---|---|---|
| [ex01](ex01-bank-account-kata/README.md) | Bank account kata ★ | Java, Clean Code, TDD, `BigDecimal` | 45 min |
| [ex02](ex02-customer-enrichment/README.md) | Customer enrichment ★★ | KStream–KTable join, tombstones, co-partitioning | 45 min |
| [ex03](ex03-currency-normalisation/README.md) | Currency normalisation ★★ | GlobalKTable join, `split()`, rounding | 45 min |
| [ex04](ex04-deduplication/README.md) | Deduplication ★★★ | Processor API, window store as TTL cache, idempotency | 60 min |
| [ex05](ex05-daily-statement/README.md) | Daily statement ★★★ | tumbling windows, grace, `suppress`, late events | 60 min |
| [ex06](ex06-dormant-accounts/README.md) | Dormant accounts ★★★★ | punctuators, wall-clock vs stream time | 75 min |

## Workflow

```bash
scripts/exercises.sh list
```

```bash
scripts/exercises.sh test ex02      # red: read the README, then make the tests green
```

```bash
scripts/exercises.sh diff ex02      # compare with the reference solution when done
```

```bash
scripts/exercises.sh start ex02     # reset to the starter code and practise again
```

`scripts/exercises.sh solve ex02` installs the reference solution. `scripts/exercises.sh check-solutions`
verifies that all solutions pass (CI runs it), then restores the starters.

The exercise tests are tagged `@Tag("exercise")` and skipped by a normal `mvn test` / `mvn verify`, so
unsolved exercises never break the build.

## How to practise like the real interview
1. Set a timer for the time in the table.
2. Read the README and write your assumptions down before coding (see [../docs/interview-playbook.md](../docs/interview-playbook.md)).
3. Make the tests green one at a time and commit after each one.
4. In the last 5 minutes, answer the "Interview follow-ups" out loud.
5. Compare with the solution, note what you would do differently, then `start` again a few days later.
