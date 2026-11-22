# Coding-challenge exercises

Six exercises in the style of real interview tasks, **already solved**: the reference solutions are
the code in `src/main` and their tests run in every build. Each one has:

| | Where |
|---|---|
| Task statement, acceptance criteria, hints, follow-up questions | `exercises/exNN-*/README.md` |
| Solved code (what the build compiles and tests) | `src/main/java/com/javabanker/exercises/exNN/` |
| Tests = the acceptance criteria | `src/test/java/com/javabanker/exercises/exNN/` |
| Reference solution (copy kept for `solve` / `diff`) | `exercises/exNN-*/solution/` |
| Starter code with `TODO`s, to re-do the exercise yourself | `exercises/exNN-*/starter/` |

| # | Exercise | Skills | Time |
|---|---|---|---|
| [ex01](ex01-bank-account-kata/README.md) | Bank account kata ★ | Java, Clean Code, TDD, `BigDecimal` | 45 min |
| [ex02](ex02-customer-enrichment/README.md) | Customer enrichment ★★ | KStream–KTable join, tombstones, co-partitioning | 45 min |
| [ex03](ex03-currency-normalisation/README.md) | Currency normalisation ★★ | GlobalKTable join, `split()`, rounding | 45 min |
| [ex04](ex04-deduplication/README.md) | Deduplication ★★★ | Processor API, window store as TTL cache, idempotency | 60 min |
| [ex05](ex05-daily-statement/README.md) | Daily statement ★★★ | tumbling windows, grace, `suppress`, late events | 60 min |
| [ex06](ex06-dormant-accounts/README.md) | Dormant accounts ★★★★ | punctuators, wall-clock vs stream time | 75 min |

## Workflow

Study a solution: read the README, then the code in `src/main/java/com/javabanker/exercises/exNN/`
and its tests, and run them:

```bash
scripts/exercises.sh test ex02
```

Practise from scratch: swap the solution for the starter code (the tests go red), then make them green:

```bash
scripts/exercises.sh start ex02
```

```bash
scripts/exercises.sh diff ex02
```

Put the reference solution back (this overwrites your attempt):

```bash
scripts/exercises.sh solve ex02
```

While an exercise is in starter state, the full build fails on its tests. Skip them with
`mvn test -Dexercise.excludedGroups=exercise`, or `solve` before committing.

## How to practise like the real interview
1. Set a timer for the time in the table.
2. Read the README and write your assumptions down before coding (see [../docs/interview-playbook.md](../docs/interview-playbook.md)).
3. Make the tests green one at a time and commit after each one.
4. In the last 5 minutes, answer the "Interview follow-ups" out loud.
5. Compare with the solution, note what you would do differently, then `start` again a few days later.
