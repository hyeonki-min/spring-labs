package min.hyeonki.ratelimiter.core;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LeakyBucketBasedWaterRateLimiter {
    private final Map<String, LeakyBucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String key, int capacity, double leakRatePerSec) {
        LeakyBucket bucket = buckets.computeIfAbsent(key,
                k -> new LeakyBucket(capacity, leakRatePerSec));
        return bucket.tryAddDrop();
    }

    private static class LeakyBucket {
        private final int capacity;
        private final double leakRatePerSec;
        private double water;
        private long lastLeakTime;

        LeakyBucket(int capacity, double leakRatePerSec) {
            this.capacity = capacity;
            this.leakRatePerSec = leakRatePerSec;
            this.water = 0;
            this.lastLeakTime = Instant.now().toEpochMilli() - 1000;
        }

        synchronized boolean tryAddDrop() {
            leak();
            if (water < capacity) {
                water += 1;
                return true;
            }
            return false;
        }

        private void leak() {
            long now = Instant.now().toEpochMilli();
            double elapsedSeconds = (now - lastLeakTime) / 1000.0;
            double leaked = elapsedSeconds * leakRatePerSec;
            if (leaked > 0) {
                water = Math.max(0, water - leaked);
                lastLeakTime = now;
            }
        }
    }
}
