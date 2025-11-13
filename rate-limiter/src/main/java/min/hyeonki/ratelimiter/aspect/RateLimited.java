package min.hyeonki.ratelimiter.aspect;

import java.lang.annotation.*;

import min.hyeonki.ratelimiter.model.Algorithm;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimited {
    Algorithm type() default Algorithm.TOKEN; // TOKEN or LEAKY
    int capacity() default 10;                // 최대 버킷 크기
    double rate() default 10.0;               // 초당 속도
}

