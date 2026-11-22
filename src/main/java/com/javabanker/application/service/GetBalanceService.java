package com.javabanker.application.service;

import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.application.port.out.BalanceRepository;
import com.javabanker.domain.AccountBalance;

import java.util.Optional;

public class GetBalanceService implements GetBalanceUseCase {

    private final BalanceRepository repository;

    public GetBalanceService(BalanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AccountBalance> balanceOf(String accountId) {
        return repository.findByAccountId(accountId);
    }
}
