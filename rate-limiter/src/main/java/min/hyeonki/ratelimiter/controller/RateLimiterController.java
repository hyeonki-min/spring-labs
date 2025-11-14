package min.hyeonki.ratelimiter.controller;

import min.hyeonki.ratelimiter.aspect.RateLimited;
import min.hyeonki.ratelimiter.model.Algorithm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/ratelimiter")
@RestController
public class RateLimiterController {

    @GetMapping("/token")
    @RateLimited(type = Algorithm.TOKEN, capacity = 10, rate = 10)
    public ResponseEntity<String> token() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/leaky/water")
    @RateLimited(type = Algorithm.LEAKY_WATER, capacity = 10, rate = 10)
    public ResponseEntity<String> leakyBucketWater() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/leaky")
    @RateLimited(type = Algorithm.LEAKY, capacity = 10, rate = 10)
    public ResponseEntity<String> leakyBucket() {
        return ResponseEntity.ok("OK");
    }
    
    @GetMapping("/fixed")
    @RateLimited(type = Algorithm.FIXED, capacity = 10)
    public ResponseEntity<String> fixedWindow() {
        return ResponseEntity.ok("OK");
    }
}