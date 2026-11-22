package com.javabanker.interview;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Classic Java interview answers, proven by executable tests (see docs/java-interview-answers.md).
 * If you can explain WHY each assertion holds, you can answer the question.
 */
class CoreJavaFactsTest {

    @Test
    void equalsVsDoubleEquals_andTheIntegerCache() {
        Integer a = 127, b = 127, c = 128, d = 128;
        assertThat(a == b).as("-128..127 are cached by Integer.valueOf").isTrue();
        assertThat(c == d).as("outside the cache: different objects").isFalse();
        assertThat(c.equals(d)).isTrue();

        String s1 = "bank";
        String s2 = new String("bank");
        assertThat(s1 == s2).isFalse();
        assertThat(s1 == s2.intern()).as("intern() returns the pooled instance").isTrue();
    }

    @Test
    void moneyNeedsBigDecimal_andCompareToNotEquals() {
        assertThat(0.1 + 0.2).isNotEqualTo(0.3); // 0.30000000000000004
        assertThat(new BigDecimal("0.1").add(new BigDecimal("0.2"))).isEqualByComparingTo("0.3");

        assertThat(new BigDecimal("1.0").equals(new BigDecimal("1.00"))).as("equals compares scale").isFalse();
        assertThat(new BigDecimal("1.0").compareTo(new BigDecimal("1.00"))).isZero();
        assertThat(new BigDecimal(0.1).toString()).as("never new BigDecimal(double)").startsWith("0.1000000000000000055");
    }

    @Test
    void equalsWithoutHashCodeBreaksHashSet() {
        class BadId {
            final String value;
            BadId(String value) { this.value = value; }
            @Override public boolean equals(Object o) { return o instanceof BadId other && other.value.equals(value); }
            // hashCode NOT overridden
        }
        record GoodId(String value) { } // records generate equals AND hashCode

        Set<BadId> bad = new HashSet<>(List.of(new BadId("x")));
        Set<GoodId> good = new HashSet<>(List.of(new GoodId("x")));

        assertThat(bad.contains(new BadId("x"))).as("different hash buckets").isFalse();
        assertThat(good.contains(new GoodId("x"))).isTrue();
    }

    @Test
    void mutableHashMapKeysGetLost() {
        List<String> key = new ArrayList<>(List.of("acc-1"));
        Map<List<String>, String> map = new HashMap<>();
        map.put(key, "Ada");

        key.add("acc-2"); // hashCode changes after insertion

        assertThat(map.get(key)).isNull();
        assertThat(map).hasSize(1); // the entry is still there, unreachable
    }

    @Test
    void failFastIterators() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4));
        assertThatThrownBy(() -> {
            for (Integer n : numbers) {
                if (n % 2 == 0) {
                    numbers.remove(n);
                }
            }
        }).isInstanceOf(ConcurrentModificationException.class);

        List<Integer> safe = new ArrayList<>(List.of(1, 2, 3, 4));
        safe.removeIf(n -> n % 2 == 0);
        assertThat(safe).containsExactly(1, 3);
    }

    @Test
    void immutableVsUnmodifiableVsFixedSize() {
        List<String> source = new ArrayList<>(List.of("a"));
        List<String> view = Collections.unmodifiableList(source);
        List<String> copy = List.copyOf(source);
        source.add("b");

        assertThat(view).as("a read-only VIEW sees changes").containsExactly("a", "b");
        assertThat(copy).as("List.of/copyOf are truly immutable").containsExactly("a");
        assertThatThrownBy(() -> copy.add("c")).isInstanceOf(UnsupportedOperationException.class);

        List<String> fixed = Arrays.asList("x", "y");
        fixed.set(0, "z"); // allowed: backed by the array
        assertThatThrownBy(() -> fixed.add("w")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void javaIsAlwaysPassByValue() {
        StringBuilder sb = new StringBuilder("a");
        reassign(sb);
        assertThat(sb.toString()).as("reassigning the parameter doesn't affect the caller").isEqualTo("a");
        mutate(sb);
        assertThat(sb.toString()).as("but the referenced object can be mutated").isEqualTo("ab");
    }

    private static void reassign(StringBuilder sb) {
        sb = new StringBuilder("changed");
    }

    private static void mutate(StringBuilder sb) {
        sb.append("b");
    }

    @Test
    void streamsAreLazy() {
        AtomicInteger evaluated = new AtomicInteger();
        Stream<Integer> pipeline = Stream.of(1, 2, 3, 4).peek(x -> evaluated.incrementAndGet()).filter(x -> x > 1);
        assertThat(evaluated).as("nothing runs without a terminal operation").hasValue(0);

        assertThat(pipeline.findFirst()).hasValue(2);
        assertThat(evaluated).as("short-circuit: stops after the first match").hasValue(2);
    }

    @Test
    void optionalOrElseIsEager_orElseGetIsLazy() {
        AtomicInteger calls = new AtomicInteger();
        Optional.of("cached").orElse(expensive(calls));
        assertThat(calls).as("orElse ALWAYS evaluates its argument").hasValue(1);

        Optional.of("cached").orElseGet(() -> expensive(calls));
        assertThat(calls).as("orElseGet only when empty").hasValue(1);
    }

    private static String expensive(AtomicInteger calls) {
        calls.incrementAndGet();
        return "computed";
    }

    @Test
    void tryWithResourcesClosesInReverseOrderAndKeepsSuppressedExceptions() {
        List<String> events = new ArrayList<>();
        class Resource implements AutoCloseable {
            final String name;
            Resource(String name) { this.name = name; }
            @Override public void close() {
                events.add("close " + name);
                throw new IllegalStateException("close failed: " + name);
            }
        }

        assertThatThrownBy(() -> {
            try (Resource first = new Resource("first"); Resource second = new Resource("second")) {
                throw new IllegalArgumentException("body failed");
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .satisfies(e -> assertThat(e.getSuppressed()).hasSize(2));
        assertThat(events).containsExactly("close second", "close first");
    }

    @Test
    void genericsAreErasedAtRuntime() {
        assertThat(new ArrayList<String>().getClass()).isEqualTo(new ArrayList<Integer>().getClass());
    }

    sealed interface Payment permits Card, Transfer { }
    record Card(String last4, BigDecimal amount) implements Payment { }
    record Transfer(String iban, BigDecimal amount) implements Payment { }

    private static String describe(Payment payment) {
        return switch (payment) { // exhaustive: no default needed, compiler checks new subtypes
            case Card(String last4, BigDecimal amount) -> "card " + last4 + " " + amount;
            case Transfer t when t.amount().compareTo(new BigDecimal("10000")) >= 0 -> "large transfer";
            case Transfer t -> "transfer to " + t.iban();
        };
    }

    @Test
    void sealedTypesRecordPatternsAndGuards() {
        assertThat(describe(new Card("1234", BigDecimal.TEN))).isEqualTo("card 1234 10");
        assertThat(describe(new Transfer("CH93", new BigDecimal("20000")))).isEqualTo("large transfer");
        assertThat(describe(new Transfer("CH93", BigDecimal.ONE))).isEqualTo("transfer to CH93");
    }

    @Test
    void atomicCounterIsCorrectUnderContention() {
        AtomicInteger counter = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            IntStream.range(0, 10_000).forEach(i -> pool.submit(counter::incrementAndGet));
        } // close() waits for all tasks (Java 19+)
        assertThat(counter).hasValue(10_000); // a plain int++ would lose updates (read-modify-write race)
    }

    @Test
    void virtualThreadsMakeTenThousandBlockingTasksCheap() {
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 10_000).forEach(i -> pool.submit(() -> {
                Thread.sleep(100);
                return i;
            }));
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(millis).as("10k × 100 ms sleeps in parallel; with 8 platform threads it would take ~125 s")
                .isLessThan(10_000);
    }

    @Test
    void hashCodeContractHelpers() {
        assertThat(Objects.hash("acc-1", 2)).isEqualTo(Objects.hash("acc-1", 2));
        assertThat(Objects.equals(null, null)).isTrue();
    }
}
