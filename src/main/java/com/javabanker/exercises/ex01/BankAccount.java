package com.javabanker.exercises.ex01;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * Exercise 01 - Bank account kata. See exercises/ex01-bank-account-kata/README.md.
 * Keep the public API (constructor + 4 methods); everything else is up to you.
 */
public class BankAccount {

    private final Supplier<LocalDate> today;

    public BankAccount(Supplier<LocalDate> today) {
        this.today = today;
    }

    public void deposit(BigDecimal amount) {
        throw new UnsupportedOperationException("TODO ex01: deposit");
    }

    public void withdraw(BigDecimal amount) {
        throw new UnsupportedOperationException("TODO ex01: withdraw");
    }

    public BigDecimal balance() {
        throw new UnsupportedOperationException("TODO ex01: balance");
    }

    public String printStatement() {
        throw new UnsupportedOperationException("TODO ex01: printStatement");
    }
}
