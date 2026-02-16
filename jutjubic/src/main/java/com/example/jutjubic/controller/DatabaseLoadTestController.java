package com.example.jutjubic.controller;

import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VideoPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/test")
public class DatabaseLoadTestController {

    private static final Logger log = LoggerFactory.getLogger(DatabaseLoadTestController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoPostRepository videoPostRepository;
    
    @Autowired
    private javax.sql.DataSource dataSource;

    private final ExecutorService executorService = Executors.newFixedThreadPool(20);

    /**
     * Simulira puno paralelnih zahteva ka bazi
     * GET /api/test/db-load?threads=10&delayMs=1000
     * 
     * @param threads
     * @param delayMs
     */
    @GetMapping("/db-load")
    public Map<String, Object> simulateDatabaseLoad(
            @RequestParam(defaultValue = "10") int threads,
            @RequestParam(defaultValue = "500") long delayMs) {
        
        final int finalThreads = Math.min(threads, 50);
        final long finalDelayMs = Math.min(delayMs, 5000L);

        log.info("Simulacija DB load-a: {} threadova, svaki sa {}ms delay", finalThreads, finalDelayMs);

        List<CompletableFuture<Long>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < finalThreads; i++) {
            final int threadNum = i;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("[Thread {}] Zapocinam query...", threadNum);
                    
                    long count = userRepository.count();
                    
                    Thread.sleep(finalDelayMs);
                    
                    log.info("[Thread {}] Query zavrsen, Pronadjeno {} korisnika", threadNum, count);
                    return count;
                } catch (InterruptedException e) {
                    log.error("[Thread {}] Prekinut", threadNum);
                    Thread.currentThread().interrupt();
                    return -1L;
                } catch (Exception e) {
                    log.error("[Thread {}] Greska: {}", threadNum, e.getMessage());
                    return -1L;
                }
            }, executorService);
            
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("Simulacija zavrsena za {}ms", duration);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("threads", finalThreads);
        response.put("delayPerQueryMs", finalDelayMs);
        response.put("totalDurationMs", duration);
        response.put("message", String.format(
            "Izvršeno %d paralelnih query-ja, svaki sa %dms delay. Trajalo: %dms",
            finalThreads, finalDelayMs, duration
        ));
        
        return response;
    }

    /**
     * Pokreni kontinuirano opterećenje u pozadini
     * GET /api/test/continuous-load?duration=30&threads=5
     * 
     * @param duration
     * @param threads
     */
    @GetMapping("/continuous-load")
    public Map<String, Object> continuousLoad(
            @RequestParam(defaultValue = "30") int duration,
            @RequestParam(defaultValue = "5") int threads) {
        
        final int finalDuration = Math.min(duration, 300);
        final int finalThreads = Math.min(threads, 20);

        log.info("Pokrecem kontinuirano opterecenje: {} threadova na {} sekundi", finalThreads, finalDuration);

        CompletableFuture.runAsync(() -> {
            long endTime = System.currentTimeMillis() + (finalDuration * 1000L);
            AtomicInteger queryCount = new AtomicInteger(0);
            
            while (System.currentTimeMillis() < endTime) {
                for (int i = 0; i < finalThreads; i++) {
                    final int queryNum = queryCount.getAndIncrement();
                    CompletableFuture.runAsync(() -> {
                        try {
                            userRepository.count();
                            videoPostRepository.count();
                            Thread.sleep(100);
                        } catch (Exception e) {
                            log.error("Greška u query {}: {}", queryNum, e.getMessage());
                        }
                    }, executorService);
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log.info("Kontinuirano opterecenje zavrseno. Izvrseno ~{} query-ja", queryCount.get());
        }, executorService);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("durationSeconds", finalDuration);
        response.put("concurrentThreads", finalThreads);
        response.put("message", String.format(
            "Kontinuirano opterećenje pokrenuto u pozadini. Trajaće %d sekundi sa %d paralelnih threadova.",
            finalDuration, finalThreads
        ));
        
        return response;
    }

    /**
     * Info o endpoint-ima
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoints", Map.of(
            "db-load", "GET /api/test/db-load?threads=10&delayMs=1000 - Jednokratno paralelno opterećenje",
            "continuous-load", "GET /api/test/continuous-load?duration=30&threads=5 - Kontinuirano opterećenje u pozadini",
            "high-load", "GET /api/test/high-load?duration=10&requestsPerSecond=200 - Simulira 200+ zahteva/sekundi",
            "slow-queries", "GET /api/test/slow-queries?connections=15&queryDuration=5 - Spori query-ji koji drže DB konekcije aktivnim",
            "info", "GET /api/test/info - Ova poruka"
        ));
        response.put("description", "Endpoint-i za testiranje DB konekcija i opterećenja sistema");
        
        return response;
    }

    /**
     * Izvršava spore query-je koji zaista drže DB konekcije aktivnim
     * GET /api/test/slow-queries?connections=15&queryDuration=5
     * 
     * Koristi pg_sleep() funkciju da zadrži konekciju aktivnom tokom celog trajanja query-ja.
     * Ovo će omogućiti da vidite aktivne konekcije u Grafani.
     * 
     * @param connections broj paralelnih konekcija (max 40)
     * @param queryDuration koliko sekundi svaki query treba da traje (max 30)
     */
    @GetMapping("/slow-queries")
    public Map<String, Object> slowQueries(
            @RequestParam(defaultValue = "15") int connections,
            @RequestParam(defaultValue = "5") int queryDuration) {
        
        final int finalConnections = Math.min(connections, 40);
        final int finalDuration = Math.min(queryDuration, 30);

        log.info("Pokrećem {} sporih query-ja, svaki traje {} sekundi", finalConnections, finalDuration);

        List<CompletableFuture<Long>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < finalConnections; i++) {
            final int threadNum = i;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("[Thread {}] Pokrećem spor query (pg_sleep {}s)...", threadNum, finalDuration);
                    
                    // Izvršava query sa pg_sleep() koji drži DB konekciju aktivnom
                    try (var connection = dataSource.getConnection();
                         var statement = connection.createStatement();
                         var resultSet = statement.executeQuery(
                             "SELECT pg_sleep(" + finalDuration + "), COUNT(*) FROM users")) {
                        
                        if (resultSet.next()) {
                            long count = resultSet.getLong(2);
                            log.info("[Thread {}] Query završen, pronađeno {} korisnika", threadNum, count);
                            successCount.incrementAndGet();
                            return count;
                        }
                    }
                    
                    return 0L;
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("[Thread {}] Greška u spором query-ju: {}", threadNum, e.getMessage());
                    return -1L;
                }
            }, executorService);
            
            futures.add(future);
            
            // Malo pauziraj između pokretanja thread-ova da se jasnije vidi na grafu
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Ne čekaj ovde - vrati odgovor odmah
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("Svi spori query-ji završeni za {}ms. Uspešno: {}, Greške: {}", 
                    totalTime, successCount.get(), errorCount.get());
            });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "running");
        response.put("connections", finalConnections);
        response.put("queryDurationSeconds", finalDuration);
        response.put("expectedTotalDurationSeconds", finalDuration + 2);
        response.put("message", String.format(
            "Pokrenuto %d sporih query-ja sa pg_sleep(%d). " +
            "DB konekcije će biti aktivne ~%d sekundi. " +
            "Proverite Grafana dashboard za metrike aktivnih konekcija!",
            finalConnections, finalDuration, finalDuration
        ));
        response.put("grafanaUrl", "http://localhost:3000/d/jutjubic-monitoring");
        
        return response;
    }

    /**
     * Simulira veliko opterećenje (200+ zahteva u sekundi)
     * GET /api/test/high-load?duration=10&requestsPerSecond=200
     * 
     * @param duration
     * @param requestsPerSecond
     */
    @GetMapping("/high-load")
    public Map<String, Object> highLoad(
            @RequestParam(defaultValue = "10") int duration,
            @RequestParam(defaultValue = "200") int requestsPerSecond) {
        
        final int finalDuration = Math.min(duration, 60);
        final int finalRps = Math.min(requestsPerSecond, 500);
        final long delayBetweenBatchesMs = 100;
        final int requestsPerBatch = (int) (finalRps * delayBetweenBatchesMs / 1000.0);

        log.info("Pokrecem HIGH LOAD test: {} req/s na {} sekundi (total ~{} zahteva)", 
            finalRps, finalDuration, finalRps * finalDuration);

        AtomicInteger completedRequests = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            long endTime = System.currentTimeMillis() + (finalDuration * 1000L);
            
            while (System.currentTimeMillis() < endTime) {
                List<CompletableFuture<Void>> batch = new ArrayList<>();
                for (int i = 0; i < requestsPerBatch; i++) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            userRepository.count();
                            completedRequests.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            log.error("Greška u high-load zahtеvu: {}", e.getMessage());
                        }
                    }, executorService);
                    batch.add(future);
                }
                
                try {
                    Thread.sleep(delayBetweenBatchesMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            long totalDuration = System.currentTimeMillis() - startTime;
            double actualRps = completedRequests.get() / (totalDuration / 1000.0);
            
            log.info("HIGH LOAD test završen: {} zahteva za {}ms (avg {} req/s), greške: {}", 
                completedRequests.get(), totalDuration, String.format("%.1f", actualRps), errorCount.get());
        }, executorService);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("durationSeconds", finalDuration);
        response.put("targetRequestsPerSecond", finalRps);
        response.put("estimatedTotalRequests", finalRps * finalDuration);
        response.put("message", String.format(
            "High-load test pokrenut: %d req/s na %d sekundi. Očekivano ~%d zahteva. Praćenje u Grafani!",
            finalRps, finalDuration, finalRps * finalDuration
        ));
        
        return response;
    }
}
