# Exercise 01 — Bank account kata ★ (Java, Clean Code, TDD) · 45 min

The classic interview kata, and the clean version of the legacy `Console/` app.

## Task
Implement `com.javabanker.exercises.ex01.BankAccount`:

```java
new BankAccount(Supplier<LocalDate> today)
void deposit(BigDecimal amount)
void withdraw(BigDecimal amount)
BigDecimal balance()
String printStatement()
```

Given a client makes a deposit of 1000 on 10/01/2026, a deposit of 2000 on 13/01/2026 and a withdrawal
of 500 on 14/01/2026, `printStatement()` returns (lines separated by `\n`, newest first):

```
date || amount || balance
14/01/2026 || -500.00 || 2500.00
13/01/2026 || 2000.00 || 3000.00
10/01/2026 || 1000.00 || 1000.00
```

## Rules / acceptance criteria
1. Amounts must be positive (`null`, `0` or negative → `IllegalArgumentException`).
2. A withdrawal larger than the balance throws `InsufficientFundsException` and changes nothing
   (no statement line, balance unchanged).
3. Withdrawing exactly the balance is allowed.
4. An account with no operations prints only the header.
5. Amounts and balances are always printed with 2 decimals.

## Constraints
- Don't change the public API used by the tests. The date comes from the injected `Supplier`, never `LocalDate.now()`.
- No Spring or Kafka. Use `BigDecimal` for money.

## Run
```bash
scripts/exercises.sh test ex01
```

<details><summary>Hints</summary>

- Keep a list of statement lines `(date, signedAmount, balanceAfter)`, so printing never recomputes anything.
- A private `record` for a statement line keeps the formatting in one place.
- Validate **before** you read the date or change any state.
</details>

## Stretch goals / interview follow-ups
- Make it thread-safe. Which is better here: `synchronized`, a lock, or immutability with an `AtomicReference`?
- Add `transferTo(BankAccount other, BigDecimal amount)`. How do you avoid deadlocks?
- Refactor `Console/src/Main.java` to use this class, with console I/O injected so it can be tested.
