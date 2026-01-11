package com.example.jutjubic.service;

import com.example.jutjubic.model.LoginAttempt;
import com.example.jutjubic.repository.LoginAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int ATTEMPT_WINDOW_MINUTES = 1;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    public void recordLoginAttempt(String ipAddress, boolean successful) {
        LoginAttempt attempt = new LoginAttempt(ipAddress, successful);
        loginAttemptRepository.save(attempt);
    }

    public boolean isBlocked(String ipAddress) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(ATTEMPT_WINDOW_MINUTES);
        List<LoginAttempt> recentAttempts = loginAttemptRepository
                .findByIpAddressAndAttemptTimeAfter(ipAddress, oneMinuteAgo);

        return recentAttempts.size() >= MAX_ATTEMPTS;
    }

    public int getRemainingAttempts(String ipAddress) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(ATTEMPT_WINDOW_MINUTES);
        List<LoginAttempt> recentAttempts = loginAttemptRepository
                .findByIpAddressAndAttemptTimeAfter(ipAddress, oneMinuteAgo);

        return Math.max(0, MAX_ATTEMPTS - recentAttempts.size());
    }
}
