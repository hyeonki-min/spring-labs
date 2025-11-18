package min.hyeonki.ratelimiter.aspect;

import jakarta.servlet.http.HttpServletRequest;
import min.hyeonki.ratelimiter.core.FixedWindowRateLimiter;
import min.hyeonki.ratelimiter.core.LeakyBucketBasedWaterRateLimiter;
import min.hyeonki.ratelimiter.core.LeakyBucketRateLimiter;
import min.hyeonki.ratelimiter.core.SlidingWindowCounterRateLimiter;
import min.hyeonki.ratelimiter.core.SlidingWindowLogRateLimiter;
import min.hyeonki.ratelimiter.core.TokenBucketRateLimiter;

import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class RateLimitAspect {

    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final LeakyBucketRateLimiter leakyBucketRateLimiter;
    private final LeakyBucketBasedWaterRateLimiter leakyBucketWaterRateLimiter;
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final SlidingWindowLogRateLimiter slidingWindowLogRateLimiter;
    private final SlidingWindowCounterRateLimiter slidingWindowCounterRateLimiter;
    private final HttpServletRequest request;

    public RateLimitAspect(
            TokenBucketRateLimiter tokenBucketRateLimiter,
            LeakyBucketRateLimiter leakyBucketRateLimiter,
            LeakyBucketBasedWaterRateLimiter leakyBucketWaterRateLimiter,
            FixedWindowRateLimiter fixedWindowRateLimiter,
            SlidingWindowLogRateLimiter slidingWindowLogRateLimiter,
            SlidingWindowCounterRateLimiter slidingWindowCounterRateLimiter,
            HttpServletRequest request
    ) {
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.leakyBucketRateLimiter = leakyBucketRateLimiter;
        this.leakyBucketWaterRateLimiter = leakyBucketWaterRateLimiter;
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.slidingWindowLogRateLimiter = slidingWindowLogRateLimiter;
        this.slidingWindowCounterRateLimiter = slidingWindowCounterRateLimiter;
        this.request = request;
    }
    
    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = request.getRemoteAddr();
        long currentTime = System.nanoTime();
        request.setAttribute("SERVER_TIME", currentTime);

        boolean allowed = switch (rateLimited.type()) {
            case TOKEN -> 
                tokenBucketRateLimiter.allowRequest(key, rateLimited.capacity(), rateLimited.rate());
            case LEAKY ->
                leakyBucketRateLimiter.allowRequest(key, rateLimited.capacity(), rateLimited.rate());
            case LEAKY_WATER ->
                leakyBucketWaterRateLimiter.allowRequest(key, rateLimited.capacity(), rateLimited.rate());
            case FIXED ->
                fixedWindowRateLimiter.allowRequest(key, rateLimited.capacity(), 1);
            case SLIDING_LOG ->
                slidingWindowLogRateLimiter.allowRequest(key, rateLimited.capacity(), 1);
            case SLIDING_COUNTER ->
                slidingWindowCounterRateLimiter.allowRequest(key, rateLimited.capacity(), 1);
        };

        if (!allowed) {
            return ResponseEntity.status(429).body(
                Map.of(
                    "serverTime", currentTime,
                    "status", 429
                )
            );
        }

        return joinPoint.proceed();
    }
}
