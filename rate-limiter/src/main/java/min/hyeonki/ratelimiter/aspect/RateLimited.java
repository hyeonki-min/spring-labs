package min.hyeonki.ratelimiter.aspect;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimited {
    int capacity() default 10;         // 최대 버킷 크기
    double refillRatePerSec() default 10.0; // 초당 토큰 생성 속도
}

