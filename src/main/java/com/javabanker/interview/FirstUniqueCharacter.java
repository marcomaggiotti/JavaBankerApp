package com.javabanker.interview;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Q: "First non-repeating character in a string."  A: LinkedHashMap keeps insertion order: two passes, O(n). */
public final class FirstUniqueCharacter {

    private FirstUniqueCharacter() {
    }

    public static Optional<Character> of(String text) {
        Map<Character, Integer> counts = new LinkedHashMap<>();
        for (char c : text.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }
        return counts.entrySet().stream().filter(e -> e.getValue() == 1).map(Map.Entry::getKey).findFirst();
    }
}
