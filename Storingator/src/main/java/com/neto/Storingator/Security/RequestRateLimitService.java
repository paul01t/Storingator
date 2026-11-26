package com.neto.Storingator.Security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RequestRateLimitService {

    /**
     * Hard bound on tracked clients. Without it, rotating source addresses grow this
     * map until the capped container is OOM-killed and restart-loops. Least-recently
     * used keys are evicted first.
     */
    static final int MAX_TRACKED_KEYS = 20_000;

    private static final class Window {
        long windowStart;
        int count;

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }

    private final Map<String, Window> windows = new LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Window> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    public synchronized boolean allow(String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        long ws = window.toMillis();

        Window w = windows.get(key);
        if (w == null || now - w.windowStart >= ws) {
            Window fresh = new Window(now);
            fresh.count = 1;
            windows.put(key, fresh);
            return 1 <= max;
        }
        w.count++;
        return w.count <= max;
    }
}
