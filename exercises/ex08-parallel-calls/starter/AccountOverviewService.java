package com.javabanker.exercises.ex08;

import com.javabanker.exercises.ex08.Clients.BalanceClient;
import com.javabanker.exercises.ex08.Clients.CardsClient;
import com.javabanker.exercises.ex08.Clients.ProfileClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Exercise 08 - correct, but calls the three services one after the other (~900 ms) and waits forever
 * for a slow cards service. See exercises/ex08-parallel-calls/README.md
 */
public class AccountOverviewService {

    private final ProfileClient profiles;
    private final BalanceClient balances;
    private final CardsClient cards;
    private final Executor executor;
    private final Duration cardsTimeout;

    public AccountOverviewService(ProfileClient profiles, BalanceClient balances, CardsClient cards,
                                  Executor executor, Duration cardsTimeout) {
        this.profiles = profiles;
        this.balances = balances;
        this.cards = cards;
        this.executor = executor;
        this.cardsTimeout = cardsTimeout;
    }

    public AccountOverview overview(String accountId) {
        String name = profiles.customerName(accountId);
        BigDecimal balance = balances.balance(accountId);
        List<String> cardNumbers;
        try {
            cardNumbers = cards.cardNumbers(accountId);
        } catch (RuntimeException e) {
            cardNumbers = List.of(); // cards are optional
        }
        return new AccountOverview(name, balance, cardNumbers);
    }
}
