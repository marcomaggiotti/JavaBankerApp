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
        // Supplier<String> as a lambda: "() ->" means no parameters; the value is computed each time get() is called.
        // Injecting the id generator (instead of calling UUID.randomUUID() inside the service) lets the unit test
        // pass "() -> \"tx-42\"" and get predictable ids: lambdas make dependencies easy to replace.
        return new SubmitTransactionService(publisher, clock, () -> UUID.randomUUID().toString());
    }

    @Bean
    GetBalanceUseCase getBalanceUseCase(BalanceRepository repository) {
        return new GetBalanceService(repository);
    }
}
