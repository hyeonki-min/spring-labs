package min.hyeonki.ratelimiter.controller;

import min.hyeonki.ratelimiter.aspect.RateLimited;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api")
@RestController
public class RateLimiterController {

    @GetMapping("/ratelimiter/token")
    @RateLimited(capacity = 10, refillRatePerSec = 10)
    public String test() {
        return "OK";
    }
}
