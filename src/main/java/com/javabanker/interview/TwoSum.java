package com.javabanker.interview;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Q: "Find two transactions whose amounts add up to the target."
 * A: one pass with a HashMap of value -> index: O(n) time, O(n) memory (the brute force is O(n²)).
 */
public final class TwoSum {

    private TwoSum() {
    }

    public record Pair(int first, int second) {
    }

    public static Optional<Pair> find(long[] amounts, long target) {
        Map<Long, Integer> seen = new HashMap<>();
        for (int i = 0; i < amounts.length; i++) {
            Integer match = seen.get(target - amounts[i]);
            if (match != null) {
                return Optional.of(new Pair(match, i));
            }
            seen.putIfAbsent(amounts[i], i);
        }
        return Optional.empty();
    }
}
