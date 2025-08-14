// Rate Limiting Service
package com.sajilni.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RateLimitService {

    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final int DEFAULT_LOCKOUT_MINUTES = 15;

    private final ConcurrentHashMap<String, LoginAttempts> loginAttempts = new ConcurrentHashMap<>();

    @Value("${app.security.rate-limit.login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.rate-limit.lockout-duration:15m}")
    private String lockoutDurationString;

    public boolean isLoginRateLimited(String email) {
        LoginAttempts attempts = loginAttempts.get(email.toLowerCase());

        if (attempts == null) {
            return false;
        }

        // Check if lockout period has expired
        if (LocalDateTime.now().isAfter(attempts.lockoutUntil())) {
            loginAttempts.remove(email.toLowerCase());
            return false;
        }

        return attempts.count() >= maxLoginAttempts;
    }

    public void recordFailedLoginAttempt(String email) {
        String key = email.toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        LoginAttempts current = loginAttempts.get(key);

        if (current == null) {
            // First failed attempt
            loginAttempts.put(key, new LoginAttempts(1, now, now.plusMinutes(DEFAULT_LOCKOUT_MINUTES)));
        } else {
            // Increment attempts
            int newCount = current.count() + 1;
            LocalDateTime lockoutUntil = newCount >= maxLoginAttempts ?
                    now.plusMinutes(DEFAULT_LOCKOUT_MINUTES) : current.lockoutUntil();

            loginAttempts.put(key, new LoginAttempts(newCount, current.firstAttempt(), lockoutUntil));
        }

        log.warn("Failed login attempt recorded for: {} (attempts: {})",
                email, loginAttempts.get(key).count());
    }

    public void resetFailedLoginAttempts(String email) {
        String removed = loginAttempts.remove(email.toLowerCase()) != null ? email : null;
        if (removed != null) {
            log.info("Reset failed login attempts for: {}", email);
        }
    }

    // Cleanup expired entries periodically
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredAttempts() {
        LocalDateTime now = LocalDateTime.now();
        AtomicInteger removed = new AtomicInteger();

        loginAttempts.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().lockoutUntil())) {
                removed.getAndIncrement();
                return true;
            }
            return false;
        });

        if (removed.get() > 0) {
            log.debug("Cleaned up {} expired login attempt records", removed);
        }
    }

    private record LoginAttempts(
            int count,
            LocalDateTime firstAttempt,
            LocalDateTime lockoutUntil
    ) {}
}
