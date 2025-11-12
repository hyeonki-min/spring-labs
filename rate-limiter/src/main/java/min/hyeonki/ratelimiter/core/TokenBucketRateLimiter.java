package min.hyeonki.ratelimiter.core;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBucketRateLimiter {

    // 사용자별 버킷 저장소
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String key, int capacity, double refillRatePerSec) {
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(capacity, refillRatePerSec));
        return bucket.tryConsume(1);
    }

    private static class TokenBucket {
        private final int capacity;
        private final double refillRatePerSec;
        private double tokens;
        private long lastRefillTimestamp;

        TokenBucket(int capacity, double refillRatePerSec) {
            this.capacity = capacity;
            this.refillRatePerSec = refillRatePerSec;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.nanoTime();
        }

        synchronized boolean tryConsume(int requestedTokens) {
            refill();
            if (tokens >= requestedTokens) {
                tokens -= requestedTokens;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1_000_000_000.0;
            double newTokens = elapsedSeconds * refillRatePerSec;
            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillTimestamp = now;
            }
        }
    }
}
