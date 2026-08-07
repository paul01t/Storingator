package com.neto.Storingator.Service;

import com.neto.Storingator.Exceptions.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRateLimitService {

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public void check(String key, int maxAttempts, Duration window) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
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

    public void recordFailure(String key, Duration window) {
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