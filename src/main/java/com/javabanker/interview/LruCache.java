package com.javabanker.interview;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Q: "Implement an LRU cache with O(1) get and put."
 * A: LinkedHashMap in access order keeps a doubly linked list over a hash table; removeEldestEntry
 * evicts the least recently used entry. (From scratch: HashMap<K, Node> + your own doubly linked list.)
 * Not thread-safe: wrap with a lock, or use Caffeine in production.
 */
public class LruCache<K, V> {

    private final Map<K, V> entries;

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.entries = new LinkedHashMap<>(16, 0.75f, true) { // true = access order
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public V get(K key) {
        return entries.get(key); // moves key to most-recently-used
    }

    public void put(K key, V value) {
        entries.put(key, value);
    }

    public int size() {
        return entries.size();
    }
}
