package com.example.jutjubic.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ActiveUserMetricsService {

    private final MeterRegistry meterRegistry;
    private final Map<String, Long> activeUsers = new ConcurrentHashMap<>();
    private final AtomicInteger activeUserCount = new AtomicInteger(0);
    private final Counter loginCounter;
    private final Counter logoutCounter;

    // Threshold za "aktivnost" - korisnik je aktivan ako je bio aktivan u poslednje 2 minuta
    private static final long ACTIVITY_THRESHOLD_MS = 2 * 60 * 1000; // 2 minuta

    public ActiveUserMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Gauge za trenutni broj aktivnih korisnika
        Gauge.builder("active.users.current", activeUserCount, AtomicInteger::get)
                .description("Current number of active users")
                .register(meterRegistry);

        // Counter za ukupan broj login-a
        this.loginCounter = Counter.builder("user.login.total")
                .description("Total number of user logins")
                .register(meterRegistry);

        // Counter za ukupan broj logout-a
        this.logoutCounter = Counter.builder("user.logout.total")
                .description("Total number of user logouts")
                .register(meterRegistry);
    }

    /**
     * Registruje aktivnost korisnika (login ili bilo koja akcija)
     */
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

    /**
     * Registruje logout korisnika
     */
    public void recordUserLogout(String username) {
        if (username != null && activeUsers.remove(username) != null) {
            activeUserCount.decrementAndGet();
            logoutCounter.increment();
        }
    }

    /**
     * Periodično čišćenje neaktivnih korisnika (svake minute)
     */
    @Scheduled(fixedRate = 60000) // svake minute
    public void cleanupInactiveUsers() {
        long currentTime = System.currentTimeMillis();
        activeUsers.entrySet().removeIf(entry -> {
            boolean isInactive = (currentTime - entry.getValue()) > ACTIVITY_THRESHOLD_MS;
            if (isInactive) {
                activeUserCount.decrementAndGet();
            }
            return isInactive;
        });
    }

    /**
     * Vraća trenutni broj aktivnih korisnika
     */
    public int getActiveUserCount() {
        return activeUserCount.get();
    }
}
