package com.javabanker.domain;

import java.math.BigDecimal;

/** Direction of money movement on an account. */
public enum TransactionType {
    DEPOSIT {
        @Override
        public BigDecimal signed(BigDecimal amount) {
            return amount;
        }
    },
    WITHDRAWAL {
        @Override
        public BigDecimal signed(BigDecimal amount) {
            return amount.negate();
        }
    };

    /** Returns the amount with the sign it has on the balance (+ deposit, - withdrawal). */
    public abstract BigDecimal signed(BigDecimal amount);
}
