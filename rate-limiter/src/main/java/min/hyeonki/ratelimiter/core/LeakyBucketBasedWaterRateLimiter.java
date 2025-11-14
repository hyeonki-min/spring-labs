package min.hyeonki.ratelimiter.core;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LeakyBucketBasedWaterRateLimiter {

    private final ConcurrentHashMap<String, AtomicReference<State>> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String key, int capacity, double leakRatePerSec) {

        AtomicReference<State> ref = buckets.computeIfAbsent(
                key,
                k -> new AtomicReference<>(State.init(0, System.nanoTime()))
        );

        while (true) {
            State prev = ref.get();

            // 1) 시간 기반 leak 적용한 새로운 상태 생성
            State afterLeak = prev.applyLeak(capacity, leakRatePerSec);

            // 2) 물을 추가해 overflow 검사
            if (afterLeak.water + 1 > capacity) {
                return false;   // capacity 초과 → 429
            }

            State updated = afterLeak.addWater(1);

            // 3) CAS 성공하면 consume 성공
            if (ref.compareAndSet(prev, updated)) {
                return true;
            }

            // 4) 실패 → 다른 스레드가 상태 갱신함 → 재시도
        }
    }

    // ------------------------
    //      IMMUTABLE STATE
    // ------------------------
    private static class State {
        final double water;
        final long lastLeakNanos;

        State(double water, long lastLeakNanos) {
            this.water = water;
            this.lastLeakNanos = lastLeakNanos;
        }

        static State init(double water, long lastLeakNanos) {
            return new State(water, lastLeakNanos);
        }

        // 시간 기반 leak 계산
        State applyLeak(int capacity, double leakRateSec) {
            long now = System.nanoTime();
            double elapsedSec = (now - lastLeakNanos) / 1_000_000_000.0;

            if (elapsedSec <= 0) {
                return this;
            }

            double leaked = elapsedSec * leakRateSec;

            double newWater = Math.max(0, water - leaked);
            return new State(newWater, now);
        }

        // 물 한 방울 추가
        State addWater(double amount) {
            return new State(water + amount, lastLeakNanos);
        }
    }
}

