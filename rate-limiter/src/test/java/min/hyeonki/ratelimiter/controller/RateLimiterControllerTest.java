package min.hyeonki.ratelimiter.controller;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RateLimiterControllerTest {
    @Autowired
    private TestRestTemplate  restTemplate;
    
    private static final int THREAD_COUNT = 20;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    @Test
    void simulateRefillScenario() throws Exception {
        List<RequestLog> logs = new CopyOnWriteArrayList<>();

        // 1️⃣ 첫 번째 요청 (동시 20개)
        sendBurstRequests(logs, "first-burst");

        // 2️⃣ 1초 대기 → 토큰 리필
        Thread.sleep(1000);

        // 3️⃣ 두 번째 요청 (동시 20개)
        sendBurstRequests(logs, "after-refill");

        // 4️⃣ 시간순 정렬 및 CSV 저장
        logs.sort((a, b) -> a.time.compareTo(b.time));
        saveAsCsv(logs);

        System.out.println("✅ rate_limit_results.csv 생성 완료");
    }

    private void sendBurstRequests(List<RequestLog> logs, String phase) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(1);

        List<Callable<Void>> tasks = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> (Callable<Void>) () -> {
                    latch.await();
                    Instant start = Instant.now();
                    ResponseEntity<String> res = restTemplate.getForEntity("/api/ratelimiter/token", String.class);
                    logs.add(new RequestLog(start, res.getStatusCode().value(), phase));
                    return null;
                })
                .toList();

        tasks.forEach(pool::submit);
        latch.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void saveAsCsv(List<RequestLog> logs) throws Exception {
        List<String> csv = new ArrayList<>();
        csv.add("timestamp,status,phase");

        for (RequestLog log : logs) {
            csv.add(FMT.format(log.time) + "," + log.status + "," + log.phase);
        }

        java.nio.file.Files.write(
                java.nio.file.Path.of("build/rate_limit_results.csv"),
                csv
        );
    }

    private static record RequestLog(Instant time, int status, String phase) {}
}
