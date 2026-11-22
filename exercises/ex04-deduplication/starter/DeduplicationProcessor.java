package com.javabanker.exercises.ex04;

import com.javabanker.domain.Transaction;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import java.time.Duration;

/** Forwards only the first occurrence of each transactionId within the window. */
class DeduplicationProcessor implements Processor<String, Transaction, String, Transaction> {

    private final String storeName;
    private final Duration window;

    DeduplicationProcessor(String storeName, Duration window) {
        this.storeName = storeName;
        this.window = window;
    }

    @Override
    public void init(ProcessorContext<String, Transaction> context) {
        // TODO ex04: keep the context and look up the state store
    }

    @Override
    public void process(Record<String, Transaction> record) {
        throw new UnsupportedOperationException("TODO ex04");
    }
}
