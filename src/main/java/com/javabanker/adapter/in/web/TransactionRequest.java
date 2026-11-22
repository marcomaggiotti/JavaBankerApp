package com.javabanker.adapter.in.web;

import com.javabanker.domain.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** API contract (DTO) - kept separate from the domain so the API can evolve independently. */
record TransactionRequest(
        @NotBlank String accountId,
        @NotNull TransactionType type,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {
}
