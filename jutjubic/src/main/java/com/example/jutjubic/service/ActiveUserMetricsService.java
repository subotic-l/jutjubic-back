package com.example.jutjubic.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ActiveUserMetricsService {

    private static final Logger log = LoggerFactory.getLogger(ActiveUserMetricsService.class);
    
    private final MeterRegistry meterRegistry;
    private final Map<String, Long> activeUsers = new ConcurrentHashMap<>();
    private final AtomicInteger activeUserCount = new AtomicInteger(0);
    private final Counter loginCounter;
    private final Counter logoutCounter;

    private static final long ACTIVITY_THRESHOLD_MS = 2 * 60 * 1000;

    public ActiveUserMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        Gauge.builder("active.users.current", activeUserCount, AtomicInteger::get)
                .description("Current number of active users")
                .register(meterRegistry);

        this.loginCounter = Counter.builder("user.login.total")
                .description("Total number of user logins")
                .register(meterRegistry);

        this.logoutCounter = Counter.builder("user.logout.total")
                .description("Total number of user logouts")
                .register(meterRegistry);
    }

    public void recordUserActivity(String username) {
        if (username != null && !username.isEmpty()) {
            boolean wasNew = !activeUsers.containsKey(username);
            activeUsers.put(username, System.currentTimeMillis());
            if (wasNew) {
                activeUserCount.incrementAndGet();
                loginCounter.increment();
            }
        }
    }

    public void recordUserLogout(String username) {
        if (username != null && activeUsers.remove(username) != null) {
            activeUserCount.decrementAndGet();
            logoutCounter.increment();
        }
    }

    @Scheduled(fixedRate = 30000)
    public void cleanupInactiveUsers() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        for (Map.Entry<String, Long> entry : activeUsers.entrySet()) {
            long inactiveTime = currentTime - entry.getValue();
            if (inactiveTime > ACTIVITY_THRESHOLD_MS) {
                activeUsers.remove(entry.getKey());
                activeUserCount.decrementAndGet();
                removedCount++;
                log.info("Korisnik {} označen kao neaktivan nakon {} ms | Ukupno aktivnih: {}", 
                    entry.getKey(), inactiveTime, activeUserCount.get());
            }
        }
        
        if (removedCount > 0) {
            log.info("Cleanup završen: Uklonjeno {} neaktivnih korisnika | Preostalo aktivnih: {}", 
                removedCount, activeUserCount.get());
        }
    }

    public int getActiveUserCount() {
        return activeUserCount.get();
    }
    
    public Map<String, Long> getActiveUsersDebugInfo() {
        return new ConcurrentHashMap<>(activeUsers);
    }
}
