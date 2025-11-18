package min.hyeonki.ratelimiter.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SlidingWindowCounterRateLimiter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public boolean allowRequest(String key, int limit, long windowSizeInSeconds) {
        long now = System.currentTimeMillis();
        WindowCounter counter = counters.computeIfAbsent(
                key, k -> new WindowCounter(windowSizeInSeconds, now)
        );
        return counter.allow(now, limit);
    }

    private static class WindowCounter {
        private final long windowSizeMillis;

        private long currentWindowStart;  // 현재 윈도우 시작 시각 (ms)
        private int currentCount;         // 현재 윈도우 요청 수
        private int previousCount;        // 직전 윈도우 요청 수

        WindowCounter(long windowSizeInSeconds, long nowMillis) {
            this.windowSizeMillis = (long) (windowSizeInSeconds * 1000L);
            this.currentWindowStart = alignedWindowStart(nowMillis);
            this.currentCount = 0;
            this.previousCount = 0;
        }

        synchronized boolean allow(long nowMillis, int limit) {
            long windowStart = alignedWindowStart(nowMillis);

            // 윈도우 이동 처리
            if (windowStart > currentWindowStart) {
                long diff = windowStart - currentWindowStart;

                if (diff == windowSizeMillis) {
                    // 정확히 한 윈도우만큼만 이동: current → previous
                    previousCount = currentCount;
                } else {
                    // 두 개 이상 윈도우를 건너뛰었으면 이전 윈도우는 의미 없음
                    previousCount = 0;
                }
                currentWindowStart = windowStart;
                currentCount = 0;
            }

            // fraction: 현재 윈도우 진행률 (0 ~ 1)
            double fraction = (double) (nowMillis - currentWindowStart) / windowSizeMillis;
            if (fraction < 0) fraction = 0;
            if (fraction > 1) fraction = 1;

            double estimated =
                    previousCount * (1.0 - fraction) +
                    currentCount;

            if (estimated >= limit) {
                return false;
            }

            currentCount++;
            return true;
        }

        private long alignedWindowStart(long timeMillis) {
            return timeMillis - (timeMillis % windowSizeMillis);
        }
    }
}
