package com.neto.Storingator.Security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequestRateLimitService {

    private static final class Window {
        long windowStart;
        int count;

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        long ws = window.toMillis();

        Window w = windows.get(key);
        if (w == null) {
            Window candidate = new Window(now);
            Window existing = windows.putIfAbsent(key, candidate);
            if (existing == null) {
                return 1 <= max;
            }
            w = existing;
        }

        boolean allowed;
        synchronized (w) {
            if (now - w.windowStart >= ws) {
                w.windowStart = now;
                w.count = 1;
                allowed = 1 <= max;
            } else {
                w.count++;
                allowed = w.count <= max;
            }
        }
        return allowed;
    }
}