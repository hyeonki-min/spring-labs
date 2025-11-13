package min.hyeonki.ratelimiter.aspect;

import jakarta.servlet.http.HttpServletRequest;
import min.hyeonki.ratelimiter.core.LeakyBucketBasedWaterRateLimiter;
import min.hyeonki.ratelimiter.core.LeakyBucketRateLimiter;
import min.hyeonki.ratelimiter.core.TokenBucketRateLimiter;
import min.hyeonki.ratelimiter.model.Algorithm;

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

    private final HttpServletRequest request;

    public RateLimitAspect(
            TokenBucketRateLimiter tokenBucketRateLimiter,
            LeakyBucketRateLimiter leakyBucketRateLimiter,
            LeakyBucketBasedWaterRateLimiter leakyBucketWaterRateLimiter,
            HttpServletRequest request
    ) {
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.leakyBucketRateLimiter = leakyBucketRateLimiter;
        this.leakyBucketWaterRateLimiter = leakyBucketWaterRateLimiter;
        this.request = request;
    }
    
    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = request.getRemoteAddr();
        boolean allowed;
        if (rateLimited.type() == Algorithm.TOKEN) {
            allowed = tokenBucketRateLimiter.allowRequest(key, rateLimited.capacity(), rateLimited.rate());
        } else if (rateLimited.type() == Algorithm.LEAKY) {
            allowed = leakyBucketRateLimiter.allowRequest(key, rateLimited.capacity(), rateLimited.rate());
        } else {
            allowed = leakyBucketWaterRateLimiter.allowRequest(key, rateLimited.capacity(), rateLimited.rate());
        }

        if (!allowed) {
            return ResponseEntity.status(429).body("Too Many Requests");
        }

        return joinPoint.proceed();
    }
}
