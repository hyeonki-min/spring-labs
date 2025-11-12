package min.hyeonki.ratelimiter.aspect;

import jakarta.servlet.http.HttpServletRequest;
import min.hyeonki.ratelimiter.core.TokenBucketRateLimiter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RateLimitAspect {

    private final TokenBucketRateLimiter rateLimiter;
    private final HttpServletRequest request;

    public RateLimitAspect(
            TokenBucketRateLimiter tokenBucketRateLimiter,
            HttpServletRequest request
    ) {
        this.rateLimiter = tokenBucketRateLimiter;
        this.request = request;
    }

    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = request.getRemoteAddr();

        boolean allowed = rateLimiter.allowRequest(
                key,
                rateLimited.capacity(),
                rateLimited.refillRatePerSec()
        );

        if (!allowed) {
            return ResponseEntity.status(429).body("Too Many Requests");
        }

        return joinPoint.proceed();
    }
}
