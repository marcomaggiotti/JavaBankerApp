package com.javabanker.interview;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** The coding questions of docs/java-interview-answers.md, with their edge cases. */
class CodingQuestionsTest {

    @Test
    void lruCacheEvictsTheLeastRecentlyUsedEntry() {
        LruCache<String, Integer> cache = new LruCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.get("a");        // a is now most recently used
        cache.put("c", 3);     // evicts b

        assertThat(cache.get("b")).isNull();
        assertThat(cache.get("a")).isEqualTo(1);
        assertThat(cache.get("c")).isEqualTo(3);
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void balancedBrackets() {
        assertThat(BalancedBrackets.isBalanced("{[()()]}")).isTrue();
        assertThat(BalancedBrackets.isBalanced("")).isTrue();
        assertThat(BalancedBrackets.isBalanced("([)]")).isFalse();
        assertThat(BalancedBrackets.isBalanced("((")).isFalse();
        assertThat(BalancedBrackets.isBalanced("())")).isFalse();
    }

    @Test
    void twoSum() {
        assertThat(TwoSum.find(new long[]{30, 70, 20, 50}, 100)).hasValue(new TwoSum.Pair(0, 1));
        assertThat(TwoSum.find(new long[]{50, 50}, 100)).hasValue(new TwoSum.Pair(0, 1));
        assertThat(TwoSum.find(new long[]{50}, 100)).isEmpty();
    }

    @Test
    void topMerchantsByFrequencyThenName() {
        var merchants = List.of("coop", "migros", "coop", "aldi", "migros", "coop", "zara", "aldi");

        assertThat(TopMerchants.top(merchants, 2)).containsExactly("coop", "aldi");
    }

    @Test
    void firstUniqueCharacter() {
        assertThat(FirstUniqueCharacter.of("swiss")).hasValue('w');
        assertThat(FirstUniqueCharacter.of("aabb")).isEmpty();
    }

    @Test
    void boundedBufferHandsOverEveryItemExactlyOnce() throws Exception {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(4);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int p = 0; p < 4; p++) {
                int producer = p;
                pool.submit(() -> {
                    for (int i = 0; i < 250; i++) {
                        buffer.put(producer * 1000 + i);
                    }
                    return null;
                });
            }
            for (int c = 0; c < 4; c++) {
                pool.submit(() -> {
                    for (int i = 0; i < 250; i++) {
                        consumed.add(buffer.take());
                    }
                    return null;
                });
            }
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(consumed).hasSize(1000).doesNotHaveDuplicates();
    }
}
