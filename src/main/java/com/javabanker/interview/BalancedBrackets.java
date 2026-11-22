package com.javabanker.interview;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Q: "Check whether ()[]{} are balanced."  A: push openers on a stack, pop on closers. O(n) time.
 * Use ArrayDeque, not the legacy synchronized Stack class.
 */
public final class BalancedBrackets {

    private static final Map<Character, Character> PAIRS = Map.of(')', '(', ']', '[', '}', '{');

    private BalancedBrackets() {
    }

    public static boolean isBalanced(String text) {
        Deque<Character> open = new ArrayDeque<>();
        for (char c : text.toCharArray()) {
            if (PAIRS.containsValue(c)) {
                open.push(c);
            } else if (PAIRS.containsKey(c)) {
                if (open.isEmpty() || open.pop() != PAIRS.get(c)) {
                    return false;
                }
            }
        }
        return open.isEmpty();
    }
}
