package com.javabanker.exercises.ex07;

import java.math.BigDecimal;

/** A slow remote FX service (~200 ms per call, rate-limited, billed per request). */
public interface FxRateClient {

    /** @return how many EUR one unit of {@code currency} is worth, or null if the currency is unknown */
    BigDecimal fetchRateToEur(String currency);
}
