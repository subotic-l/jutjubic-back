package com.example.jutjubic.controller;

import com.example.jutjubic.service.ActiveUserMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private ActiveUserMetricsService activeUserMetricsService;

    private static final DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
            "status", "UP",
            "message", "Jutjubic API is running!"
        );
    }

    @GetMapping("/heartbeat")
    public Map<String, String> heartbeat() {
        return Map.of(
            "status", "OK",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }

    @GetMapping("/debug/active-users")
    public Map<String, Object> debugActiveUsers() {
        Map<String, Long> activeUsers = activeUserMetricsService.getActiveUsersDebugInfo();
        Map<String, Object> result = new HashMap<>();
        Map<String, String> usersWithTimes = new HashMap<>();
        
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, Long> entry : activeUsers.entrySet()) {
            long lastActivityTime = entry.getValue();
            long inactiveForMs = currentTime - lastActivityTime;
            long inactiveForSeconds = inactiveForMs / 1000;
            
            String lastActivityFormatted = formatter.format(Instant.ofEpochMilli(lastActivityTime));
            
            usersWithTimes.put(
                entry.getKey(), 
                String.format("Poslednja aktivnost: %s (%d sekundi pre)", 
                    lastActivityFormatted, inactiveForSeconds)
            );
        }
        
        result.put("totalActiveUsers", activeUserMetricsService.getActiveUserCount());
        result.put("activeUsers", usersWithTimes);
        result.put("currentTime", formatter.format(Instant.ofEpochMilli(currentTime)));
        
        return result;
    }
}
