package com.javabanker.exercises.ex01;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Reference solution for exercise 01. */
public class BankAccount {

    private static final String HEADER = "date || amount || balance";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Supplier<LocalDate> today;
    private final List<StatementLine> lines = new ArrayList<>();
    private BigDecimal balance = BigDecimal.ZERO;

    public BankAccount(Supplier<LocalDate> today) {
        this.today = today;
    }

    public void deposit(BigDecimal amount) {
        record(requirePositive(amount));
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Cannot withdraw " + amount + ", balance is " + balance);
        }
        record(amount.negate());
    }

    public BigDecimal balance() {
        return balance;
    }

    public String printStatement() {
        String body = IntStream.range(0, lines.size())
                .mapToObj(i -> lines.get(lines.size() - 1 - i)) // newest first
                .map(StatementLine::format)
                .collect(Collectors.joining("\n"));
        return body.isEmpty() ? HEADER : HEADER + "\n" + body;
    }

    private void record(BigDecimal signedAmount) {
        balance = balance.add(signedAmount);
        lines.add(new StatementLine(today.get(), signedAmount, balance));
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive but was " + amount);
        }
        return amount;
    }

    private record StatementLine(LocalDate date, BigDecimal amount, BigDecimal balance) {
        String format() {
            return DATE.format(date) + " || " + money(amount) + " || " + money(balance);
        }

        private static String money(BigDecimal value) {
            return value.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
        }
    }
}
