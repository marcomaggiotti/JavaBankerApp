package com.javabanker.exercises.ex04;

import com.javabanker.domain.Transaction;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;

import java.time.Duration;

/** Forwards only the first occurrence of each transactionId within the window (event time). */
class DeduplicationProcessor implements Processor<String, Transaction, String, Transaction> {

    private final String storeName;
    private final Duration window;
    private ProcessorContext<String, Transaction> context;
    private WindowStore<String, Long> seen;

    DeduplicationProcessor(String storeName, Duration window) {
        this.storeName = storeName;
        this.window = window;
    }

    @Override
    public void init(ProcessorContext<String, Transaction> context) {
        this.context = context;
        this.seen = context.getStateStore(storeName);
    }

    @Override
    public void process(Record<String, Transaction> record) {
        Transaction tx = record.value();
        if (tx == null) {
            return;
        }
        long timestamp = record.timestamp();
        if (seenWithinWindow(tx.transactionId(), timestamp)) {
            return;
        }
        seen.put(tx.transactionId(), timestamp, timestamp);
        context.forward(record);
    }

    private boolean seenWithinWindow(String transactionId, long timestamp) {
        try (WindowStoreIterator<Long> hits = seen.fetch(transactionId, timestamp - window.toMillis(), timestamp)) {
            return hits.hasNext();
        }
    }
}
