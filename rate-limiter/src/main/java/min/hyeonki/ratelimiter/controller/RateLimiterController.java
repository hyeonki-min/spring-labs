package min.hyeonki.ratelimiter.controller;

import min.hyeonki.ratelimiter.aspect.RateLimited;
import min.hyeonki.ratelimiter.model.Algorithm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api")
@RestController
public class RateLimiterController {

    @GetMapping("/ratelimiter/token")
    @RateLimited(type = Algorithm.TOKEN, capacity = 10, rate = 10)
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/ratelimiter/leaky/water")
    @RateLimited(type = Algorithm.LEAKY_WATER, capacity = 10, rate = 10)
    public ResponseEntity<String> leakyBucketWaterTest() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/ratelimiter/leaky")
    @RateLimited(type = Algorithm.LEAKY, capacity = 10, rate = 10)
    public ResponseEntity<String> leakyBucketTest() {
        return ResponseEntity.ok("OK");
    }
}
