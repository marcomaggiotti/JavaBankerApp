package com.javabanker.exercises.ex01;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("exercise")
class BankAccountTest {

    private final AtomicReference<LocalDate> today = new AtomicReference<>(LocalDate.of(2026, 1, 10));
    private final BankAccount account = new BankAccount(today::get);

    @Test
    void printsStatementNewestFirst() {
        account.deposit(new BigDecimal("1000"));
        today.set(LocalDate.of(2026, 1, 13));
        account.deposit(new BigDecimal("2000"));
        today.set(LocalDate.of(2026, 1, 14));
        account.withdraw(new BigDecimal("500"));

        assertThat(account.printStatement()).isEqualTo("""
                date || amount || balance
                14/01/2026 || -500.00 || 2500.00
                13/01/2026 || 2000.00 || 3000.00
                10/01/2026 || 1000.00 || 1000.00""");
        assertThat(account.balance()).isEqualByComparingTo("2500");
    }

    @Test
    void emptyAccountPrintsOnlyHeader() {
        assertThat(account.printStatement()).isEqualTo("date || amount || balance");
        assertThat(account.balance()).isEqualByComparingTo("0");
    }

    @Test
    void overdraftIsRejectedAndChangesNothing() {
        account.deposit(new BigDecimal("50"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("50.01")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.balance()).isEqualByComparingTo("50");
        assertThat(account.printStatement().lines()).hasSize(2);
    }

    @Test
    void withdrawingExactBalanceIsAllowed() {
        account.deposit(new BigDecimal("50"));
        account.withdraw(new BigDecimal("50"));

        assertThat(account.balance()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.deposit(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.withdraw(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(account.printStatement()).isEqualTo("date || amount || balance");
    }

    @Test
    void amountsAlwaysHaveTwoDecimals() {
        account.deposit(new BigDecimal("12.5"));

        assertThat(account.printStatement()).endsWith("10/01/2026 || 12.50 || 12.50");
    }
}
