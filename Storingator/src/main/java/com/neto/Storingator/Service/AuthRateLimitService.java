package com.neto.Storingator.Service;

import com.neto.Storingator.Exceptions.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthRateLimitService {

    /** Bound per-username/per-signup-key state under arbitrary public inputs. */
    public static final int MAX_TRACKED_KEYS = 20_000;

    private final Map<String, Deque<Long>> windows = new LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Deque<Long>> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    public synchronized void check(String key, int maxAttempts, Duration window) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.get(key);
        if (deque == null) {
            return;
        }
        synchronized (deque) {
            prune(deque, now, window);
            if (deque.isEmpty()) {
                windows.remove(key, deque);
            }
            if (deque.size() >= maxAttempts) {
                throw new TooManyRequestsException("Too many attempts. Please try again later.");
            }
        }
    }

    public synchronized void recordFailure(String key, Duration window) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            prune(deque, now, window);
            deque.addLast(now);
        }
    }

    private void prune(Deque<Long> deque, long now, Duration window) {
        long cutoff = now - window.toMillis();
        while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
            deque.pollFirst();
        }
    }
}
