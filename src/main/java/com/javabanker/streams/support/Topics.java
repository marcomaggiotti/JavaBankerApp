package com.javabanker.streams.support;

/** Topic names, bound from {@code banker.topics.*}. Versioned names make breaking changes explicit. */
public record Topics(String transactions, String balances, String rejected, String fraudAlerts) {

    public static Topics defaults() {
        return new Topics(
                "bank.transactions.v1",
                "bank.balances.v1",
                "bank.transactions.rejected.v1",
                "bank.fraud-alerts.v1");
    }
}
