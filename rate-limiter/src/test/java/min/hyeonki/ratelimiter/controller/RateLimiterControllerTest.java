package min.hyeonki.ratelimiter.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RateLimiterControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper mapper;

    private static final int THREAD_COUNT = 20;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    private static record RequestLog(Instant clientTime, long serverTime, int status, String phase, String algorithm) {}

    @Test
    void testTokenBucket() throws Exception {
        runRateLimiterTest(
                "/api/ratelimiter/token",
                "token",
                "build/token_bucket_results.csv"
        );
    }

    @Test
    void testLeakyBucket() throws Exception {
        runRateLimiterTest(
                "/api/ratelimiter/leaky",
                "leaky",
                "build/leaky_bucket_results.csv"
        );
    }

    @Test
    void testLeakyBucketBasedWater() throws Exception {
        runRateLimiterTest(
                "/api/ratelimiter/leaky/water",
                "leaky_water",
                "build/leaky_bucket_water_results.csv"
        );
    }

    @Test
    void testFixedWindow() throws Exception {
        runRateLimiterTest(
                "/api/ratelimiter/fixed",
                "fixed",
                "build/fixed_window_results.csv"
        );
    }

    @Test
    void testSlidingWindowLog() throws Exception {
        runRateLimiterTest(
                "/api/ratelimiter/sliding-log",
                "sliding-log",
                "build/sliding_window_log_results.csv"
        );
    }

    /**
     * 재사용 가능한 공통 테스트 로직
     */
    private void runRateLimiterTest(String endpoint, String algorithm, String outputFile) throws Exception {
        Queue<RequestLog> logs = new ConcurrentLinkedQueue<>();

        // 첫 burst
        sendBurstRequests(logs, "first-burst", algorithm, endpoint);

        // 1초 대기 (리필 또는 누출 효과)
        Thread.sleep(1000);

        // 두 번째 burst
        sendBurstRequests(logs, "after-refill", algorithm, endpoint);

        List<RequestLog> sorted = logs.stream()
            .sorted(Comparator.comparing(RequestLog::serverTime))
            .toList();

        saveAsCsv(sorted, outputFile);

        System.out.printf("✅ %s 생성 완료%n", outputFile);
    }

    private void sendBurstRequests(
            Queue<RequestLog> logs,
            String phase,
            String algorithm,
            String endpoint
    ) throws InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(1);

        List<Callable<Void>> tasks =
                IntStream.range(0, THREAD_COUNT)
                        .mapToObj(i -> (Callable<Void>) () -> {
                            latch.await();
                            Instant clientStart = Instant.now();
                            ResponseEntity<String> res =
                                    restTemplate.getForEntity(endpoint, String.class);
                            String body = res.getBody();
                            long serverTime = extractServerTime(body);

                            logs.add(new RequestLog(
                                    clientStart,
                                    serverTime,
                                    res.getStatusCode().value(),
                                    phase,
                                    algorithm
                            ));
                            return null;
                        })
                        .toList();

        tasks.forEach(pool::submit);
        latch.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void saveAsCsv(List<RequestLog> logs, String filePath) throws Exception {
        List<String> csv = new ArrayList<>();
        csv.add("client_time,server_time,status,phase,algorithm");

        for (RequestLog log : logs) {
            csv.add(
                FMT.format(log.clientTime()) + "," +
                log.serverTime() + "," +
                log.status() + "," +
                log.phase() + "," +
                log.algorithm()
            );
        }

        Files.write(Path.of(filePath), csv);
    }

    private long extractServerTime(String body) {
        try {
            JsonNode node = mapper.readTree(body);
            return node.get("serverTime").asLong();
        } catch (Exception e) {
            return -1; // 실패 시 표시용
        }
    }
}
