package com.javabanker.streams.topology;

import com.javabanker.domain.AccountBalance;
import com.javabanker.domain.Transaction;
import com.javabanker.domain.TransactionOutcome;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Stateful processor (Processor API) that applies each transaction to the stored balance.
 * <p>
 * Why not {@code groupByKey().aggregate()}? An aggregate always produces a new value - it cannot
 * "reject" an input and route it elsewhere. Here we need to forward an outcome (accepted OR rejected),
 * so we read/write the state store ourselves. The business rule itself lives in the domain.
 */
class BalanceProcessor implements Processor<String, Transaction, String, TransactionOutcome> {

    private final String storeName;
    private ProcessorContext<String, TransactionOutcome> context;
    private KeyValueStore<String, AccountBalance> store;

    BalanceProcessor(String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void init(ProcessorContext<String, TransactionOutcome> context) {
        this.context = context;
        this.store = context.getStateStore(storeName);
    }

    @Override
    public void process(Record<String, Transaction> record) {
        Transaction tx = record.value();
        if (tx == null) {
            return; // tombstones / poison pills are ignored here; see DeserializationExceptionHandler
        }
        AccountBalance current = store.get(tx.accountId());
        if (current == null) {
            current = AccountBalance.open(tx.accountId(), tx.currency());
        }
        TransactionOutcome outcome = current.apply(tx);
        if (outcome.isAccepted()) {
            store.put(tx.accountId(), outcome.balance());
        }
        context.forward(record.withKey(tx.accountId()).withValue(outcome));
    }
}
