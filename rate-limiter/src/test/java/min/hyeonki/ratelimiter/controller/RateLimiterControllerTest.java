package min.hyeonki.ratelimiter.controller;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    private TestRestTemplate restTemplate;

    private static final int THREAD_COUNT = 20;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    private static record RequestLog(Instant time, int status, String phase, String algorithm) {}

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

    /**
     * 재사용 가능한 공통 테스트 로직
     */
    private void runRateLimiterTest(String endpoint, String algorithm, String outputFile) throws Exception {
        List<RequestLog> logs = new CopyOnWriteArrayList<>();

        // 첫 burst
        sendBurstRequests(logs, "first-burst", algorithm, endpoint);

        // 1초 대기 (리필 또는 누출 효과)
        Thread.sleep(1000);

        // 두 번째 burst
        sendBurstRequests(logs, "after-refill", algorithm, endpoint);

        logs.sort(Comparator.comparing(a -> a.time));
        saveAsCsv(logs, outputFile);

        System.out.printf("✅ %s 생성 완료%n", outputFile);
    }

    private void sendBurstRequests(
            List<RequestLog> logs,
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
                            Instant start = Instant.now();
                            ResponseEntity<String> res =
                                    restTemplate.getForEntity(endpoint, String.class);
                            logs.add(new RequestLog(start, res.getStatusCode().value(), phase, algorithm));
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
        csv.add("timestamp,status,phase,algorithm");

        for (RequestLog log : logs) {
            csv.add(FMT.format(log.time) + "," + log.status + "," + log.phase + "," + log.algorithm);
        }

        java.nio.file.Files.write(java.nio.file.Path.of(filePath), csv);
    }
}
