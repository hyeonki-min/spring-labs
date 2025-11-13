package min.hyeonki.ratelimiter.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class LeakyBucketRateLimiter {

    private final Map<String, LeakyBucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String key, int capacity, double leakRatePerSec) {
        LeakyBucket bucket = buckets.computeIfAbsent(
                key, k -> new LeakyBucket(capacity, leakRatePerSec));
        return bucket.tryPush();
    }

    private static class LeakyBucket {
        private final int capacity;
        private final double leakRatePerSec;
        private final Deque<Long> queue; // timestamp queue
        private long lastLeakTime;

        LeakyBucket(int capacity, double leakRatePerSec) {
            this.capacity = capacity;
            this.leakRatePerSec = leakRatePerSec;
            this.queue = new ArrayDeque<>(capacity);
            this.lastLeakTime = System.nanoTime();
        }

        synchronized boolean tryPush() {
            leak();  // 먼저 일정 개수 제거

            // 큐가 꽉 차 있으면 drop = 429
            if (queue.size() >= capacity) {
                return false;
            }

            // 큐에 요청을 추가 (timestamp 저장)
            queue.addLast(System.nanoTime());
            return true;
        }

        private void leak() {
            long now = System.nanoTime();
            double elapsedSec = (now - lastLeakTime) / 1_000_000_000.0;
            int leaks = (int)(elapsedSec * leakRatePerSec);

            for (int i = 0; i < leaks && !queue.isEmpty(); i++) {
                queue.pollFirst(); // FIFO
            }

            if (leaks > 0) {
                lastLeakTime = now;
            }
        }
    }
}