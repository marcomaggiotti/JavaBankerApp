package com.javabanker.exercises.ex08;

import com.javabanker.exercises.ex08.Clients.BalanceClient;
import com.javabanker.exercises.ex08.Clients.CardsClient;
import com.javabanker.exercises.ex08.Clients.ProfileClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Reference solution: the three independent calls run concurrently, so latency = the slowest call
 * (~300 ms) instead of the sum (~900 ms). Cards is optional: on failure or after the timeout it
 * degrades to an empty list. With virtual threads the executor can be one thread per task.
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
        CompletableFuture<String> name = CompletableFuture.supplyAsync(() -> profiles.customerName(accountId), executor);
        CompletableFuture<BigDecimal> balance = CompletableFuture.supplyAsync(() -> balances.balance(accountId), executor);
        CompletableFuture<List<String>> cardNumbers = CompletableFuture
                .supplyAsync(() -> cards.cardNumbers(accountId), executor)
                .completeOnTimeout(List.of(), cardsTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(error -> List.of());
        try {
            return new AccountOverview(name.join(), balance.join(), cardNumbers.join());
        } catch (CompletionException e) {
            balance.cancel(true);
            cardNumbers.cancel(true);
            throw unwrap(e);
        }
    }

    /** join() wraps failures in CompletionException: callers must see the original exception type. */
    private static RuntimeException unwrap(CompletionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return e;
    }
}
