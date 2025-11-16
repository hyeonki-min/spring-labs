package min.hyeonki.ratelimiter.core;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Sliding Window Log 기반 인메모리 Rate Limiter.
 *
 * - windowMillis 동안의 요청 timestamp를 모두 기록
 * - allowRequest 호출 시:
 *   1) window 밖의 timestamp 제거
 *   2) 남은 개수가 limit 미만이면 허용 + 현재 timestamp 기록
 *   3) limit 이상이면 거절
 */
@Component
public class SlidingWindowLogRateLimiter {

    private final ConcurrentHashMap<String, WindowLog> logs = new ConcurrentHashMap<>();

    /**
     * @param key          사용자 ID / IP / 엔드포인트 등 구분자
     * @param limit        window 동안 허용 가능한 최대 요청 수
     * @param windowSizeInSeconds 슬라이딩 윈도우 크기 (초)
     * @return true = 허용(200), false = 거절(429)
     */
    public boolean allowRequest(String key, int limit, long windowSizeInSeconds) {
        long now = System.currentTimeMillis();
        WindowLog log = getOrCreateLog(key);
        long windowMillis = windowSizeInSeconds * 1000L;
        return log.allow(now, limit, windowMillis);
    }

    private WindowLog getOrCreateLog(String key) {
        WindowLog existing = logs.get(key);
        if (existing != null) {
            return existing;
        }

        WindowLog newLog = new WindowLog();
        WindowLog race = logs.putIfAbsent(key, newLog);
        return race != null ? race : newLog;
    }

    /**
     * 단일 key에 대한 로그 버킷
     */
    private static class WindowLog {
        // 요청 timestamp 목록 (ms)
        private final Deque<Long> timestamps = new ArrayDeque<>();

        /**
         * thread-safe: 한 key에 대해서는 동시 접근 시 직렬화
         */
        synchronized boolean allow(long nowMillis, int limit, long windowMillis) {
            long boundary = nowMillis - windowMillis;

            // 1) 윈도우 밖(오래된) 요청들 정리
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= boundary) {
                timestamps.pollFirst();
            }

            // 2) 현재 윈도우 내 요청 수 확인
            if (timestamps.size() >= limit) {
                // 이미 꽉 찼으니 거절
                return false;
            }

            // 3) 아직 여유가 있으니 허용 + 현재 요청 기록
            timestamps.addLast(nowMillis);
            return true;
        }
    }
}
