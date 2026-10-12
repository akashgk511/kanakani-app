package com.kanakani.security;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window limiter, for example "3 OTP requests per phone token per 10 minutes".
 * It lives in memory, so it works for a single server. Use Redis when you run more than one.
 */
public final class RateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Returns true and records the hit if the caller is under the limit. */
    public boolean allow(String key, int maxHits, Duration window) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < cutoff) {
                q.pollFirst();
            }
            if (q.size() >= maxHits) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }
}
