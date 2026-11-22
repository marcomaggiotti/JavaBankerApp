package com.javabanker.application.service;

import com.javabanker.application.port.in.SubmitTransactionUseCase;
import com.javabanker.application.port.out.TransactionPublisher;
import com.javabanker.domain.Transaction;

import java.time.Clock;
import java.util.function.Supplier;

/**
 * Use case implementation. Note: no Spring annotations - it is wired in {@code UseCaseConfig}, so the
 * application core stays framework-agnostic (enforced by {@code ArchitectureTest}).
 */
public class SubmitTransactionService implements SubmitTransactionUseCase {

    private final TransactionPublisher publisher;
    private final Clock clock;
    private final Supplier<String> idGenerator;

    public SubmitTransactionService(TransactionPublisher publisher, Clock clock, Supplier<String> idGenerator) {
        this.publisher = publisher;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Override
    public Transaction submit(SubmitTransactionCommand command) {
        Transaction tx = new Transaction(
                idGenerator.get(),
                command.accountId(),
                command.type(),
                command.amount(),
                command.currency(),
                clock.instant());
        publisher.publish(tx);
        return tx;
    }
}
