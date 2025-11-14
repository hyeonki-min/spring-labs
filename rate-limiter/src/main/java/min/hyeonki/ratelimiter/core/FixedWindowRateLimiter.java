package min.hyeonki.ratelimiter.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class FixedWindowRateLimiter {

    private static class Window {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long windowStart) {
            this.windowStart = windowStart;
        }

        boolean tryAcquire(int limit) {
            while (true) {
                int current = count.get();
                if (current >= limit) return false;
                if (count.compareAndSet(current, current + 1)) return true;
            }
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @param key     rateLimit key(IP/UserId/Endpoint)
     * @param limit   윈도우 내 최대 허용 요청 수
     * @param windowSizeInSeconds 윈도우 사이즈 (기본 1초)
     */
    public boolean allowRequest(String key, int limit, int windowSizeInSeconds) {
        long now = System.currentTimeMillis();
        long windowSizeMillis = windowSizeInSeconds * 1000;
        long windowStart = now / windowSizeMillis * windowSizeMillis;

        Window window = windows.compute(key, (k, w) -> {
            if (w == null || w.windowStart != windowStart) {
                return new Window(windowStart);
            }
            return w;
        });

        return window.tryAcquire(limit);
    }
}
