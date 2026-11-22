package com.javabanker.config;

import com.javabanker.application.port.in.GetBalanceUseCase;
import com.javabanker.application.port.in.SubmitTransactionUseCase;
import com.javabanker.application.port.out.BalanceRepository;
import com.javabanker.application.port.out.TransactionPublisher;
import com.javabanker.application.service.GetBalanceService;
import com.javabanker.application.service.SubmitTransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Composition root for the framework-free application core. */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SubmitTransactionUseCase submitTransactionUseCase(TransactionPublisher publisher, Clock clock) {
        return new SubmitTransactionService(publisher, clock, () -> UUID.randomUUID().toString());
    }

    @Bean
    GetBalanceUseCase getBalanceUseCase(BalanceRepository repository) {
        return new GetBalanceService(repository);
    }
}
