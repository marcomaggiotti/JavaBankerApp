package com.javabanker.exercises.ex03;

import java.math.BigDecimal;

/** Value of the compacted topic bank.fx-rates.v1 (key = currency). 1 unit of currency = rateToEur EUR. */
public record FxRate(String currency, BigDecimal rateToEur) {
}
