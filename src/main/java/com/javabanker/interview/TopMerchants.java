package com.javabanker.interview;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Q: "Given a list of merchant names, return the k most frequent (ties alphabetically)."
 * A: groupingBy + counting, then sort by count desc / name asc and take k. O(n + m log m).
 * For huge m and small k: a min-heap (PriorityQueue) of size k gives O(m log k).
 */
public final class TopMerchants {

    private TopMerchants() {
    }

    public static List<String> top(Collection<String> merchants, int k) {
        Map<String, Long> counts = merchants.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(k)
                .map(Map.Entry::getKey)
                .toList();
    }
}
