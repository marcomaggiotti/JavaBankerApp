package com.javabanker.exercises.ex08;

import java.math.BigDecimal;
import java.util.List;

/** Three independent remote services, each with ~300 ms latency. */
public final class Clients {

    private Clients() {
    }

    public interface ProfileClient {
        /** @throws AccountNotFoundException if the account doesn't exist */
        String customerName(String accountId);
    }

    public interface BalanceClient {
        BigDecimal balance(String accountId);
    }

    public interface CardsClient {
        List<String> cardNumbers(String accountId);
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String accountId) {
            super("Unknown account " + accountId);
        }
    }
}
