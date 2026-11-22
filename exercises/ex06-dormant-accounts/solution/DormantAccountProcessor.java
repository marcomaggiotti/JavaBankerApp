package com.javabanker.exercises.ex06;

import com.javabanker.domain.Transaction;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class DormantAccountProcessor implements Processor<String, Transaction, String, DormantAccountAlert> {

    private final String storeName;
    private final Duration inactivity;
    private final Duration checkInterval;
    private ProcessorContext<String, DormantAccountAlert> context;
    private KeyValueStore<String, AccountActivity> activity;

    DormantAccountProcessor(String storeName, Duration inactivity, Duration checkInterval) {
        this.storeName = storeName;
        this.inactivity = inactivity;
        this.checkInterval = checkInterval;
    }

    @Override
    public void init(ProcessorContext<String, DormantAccountAlert> context) {
        this.context = context;
        this.activity = context.getStateStore(storeName);
        // WALL_CLOCK_TIME: fires even when no new records arrive (STREAM_TIME would not!)
        context.schedule(checkInterval, PunctuationType.WALL_CLOCK_TIME, this::detectDormantAccounts);
    }

    @Override
    public void process(Record<String, Transaction> record) {
        Transaction tx = record.value();
        if (tx == null) {
            return;
        }
        activity.put(tx.accountId(), new AccountActivity(Instant.ofEpochMilli(record.timestamp()), false));
    }

    private void detectDormantAccounts(long now) {
        List<KeyValue<String, AccountActivity>> dormant = new ArrayList<>();
        try (KeyValueIterator<String, AccountActivity> all = activity.all()) {
            while (all.hasNext()) {
                KeyValue<String, AccountActivity> entry = all.next();
                AccountActivity a = entry.value;
                if (!a.alerted() && now - a.lastActivity().toEpochMilli() >= inactivity.toMillis()) {
                    dormant.add(entry);
                }
            }
        }
        for (KeyValue<String, AccountActivity> entry : dormant) { // modify the store after closing the iterator
            activity.put(entry.key, entry.value.markAlerted());
            context.forward(new Record<>(entry.key,
                    new DormantAccountAlert(entry.key, entry.value.lastActivity(), Instant.ofEpochMilli(now)), now));
        }
    }
}
