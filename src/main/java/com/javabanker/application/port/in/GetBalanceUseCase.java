package com.javabanker.application.port.in;

import com.javabanker.domain.AccountBalance;

import java.util.Optional;

/** Inbound port: read the materialized balance of an account. */
public interface GetBalanceUseCase {

    Optional<AccountBalance> balanceOf(String accountId);
}
