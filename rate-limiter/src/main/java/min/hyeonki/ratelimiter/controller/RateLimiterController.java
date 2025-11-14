package min.hyeonki.ratelimiter.controller;

import min.hyeonki.ratelimiter.aspect.RateLimited;
import min.hyeonki.ratelimiter.model.Algorithm;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RequestMapping("/api/ratelimiter")
@RestController
public class RateLimiterController {

    @GetMapping("/token")
    @RateLimited(type = Algorithm.TOKEN, capacity = 10, rate = 10)
    public ResponseEntity<Map<String, Object>> token(HttpServletRequest request) {
        long serverTime = (long) request.getAttribute("SERVER_TIME");
        return ResponseEntity.ok(
            Map.of(
                "status", "OK",
                "serverTime", serverTime
            )
        );
    }

    @GetMapping("/leaky/water")
    @RateLimited(type = Algorithm.LEAKY_WATER, capacity = 10, rate = 10)
    public ResponseEntity<Map<String, Object>> leakyBucketWater(HttpServletRequest request) {
        long serverTime = (long) request.getAttribute("SERVER_TIME");
        return ResponseEntity.ok(
            Map.of(
                "status", "OK",
                "serverTime", serverTime
            )
        );
    }

    @GetMapping("/leaky")
    @RateLimited(type = Algorithm.LEAKY, capacity = 10, rate = 10)
    public ResponseEntity<Map<String, Object>> leakyBucket(HttpServletRequest request) {
        long serverTime = (long) request.getAttribute("SERVER_TIME");
        return ResponseEntity.ok(
            Map.of(
                "status", "OK",
                "serverTime", serverTime
            )
        );
    }
    
    @GetMapping("/fixed")
    @RateLimited(type = Algorithm.FIXED, capacity = 10)
    public ResponseEntity<Map<String, Object>> fixedWindow(HttpServletRequest request) {
        long serverTime = (long) request.getAttribute("SERVER_TIME");
        return ResponseEntity.ok(
            Map.of(
                "status", "OK",
                "serverTime", serverTime
            )
        );
    }
}