package com.sajilni.service;

import com.sajilni.domain.constants.Type;
import com.sajilni.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class OtpService {
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int MAX_GENERATION_ATTEMPTS_PER_HOUR = 3;
    private static final int LOCKOUT_MINUTES = 15;

    private final ConcurrentHashMap<String, OtpChallenge> otpStorage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GenerationAttempts> generationAttempts = new ConcurrentHashMap<>();

    @Value("${app.otp.length:6}")
    private int length;

    @Value("${app.otp.ttl-minutes:30}")
    private int ttlMinutes;

    @Value("${app.otp.type:NUMERIC}")
    private Type type;

    /**
     * Generate OTP with challenge tracking
     */
    public String generate(String email) {
        String normalizedEmail = email.toLowerCase();

        // Check generation rate limiting
        if (isGenerationRateLimited(normalizedEmail)) {
            throw new BusinessException("otp.rate.limited",
                    "Too many OTP generation attempts. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        String code = randomToken(length, type);
        String key = buildKey(normalizedEmail);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(ttlMinutes);

        // Create challenge with attempt tracking
        OtpChallenge challenge = new OtpChallenge(
                code,
                expiryTime,
                0,
                MAX_VERIFICATION_ATTEMPTS,
                LocalDateTime.now(),
                false
        );

        otpStorage.put(key, challenge);
        updateGenerationAttempts(normalizedEmail);

        log.debug("OTP challenge created for {}, expires at {}, max attempts: {}",
                normalizedEmail, expiryTime, MAX_VERIFICATION_ATTEMPTS);

        return code;
    }

    /**
     * Verify OTP with challenge validation
     */
    public boolean verify(String email, String otp) {
        String normalizedEmail = email.toLowerCase();
        String key = buildKey(normalizedEmail);
        OtpChallenge challenge = otpStorage.get(key);

        if (challenge == null) {
            log.debug("No OTP challenge found for email: {}", normalizedEmail);
            return false;
        }

        // Check if challenge is locked due to too many attempts
        if (challenge.locked()) {
            log.debug("OTP challenge locked for email: {}", normalizedEmail);
            throw new BusinessException("otp.locked",
                    "Too many incorrect attempts. Please request a new OTP.",
                    HttpStatus.FORBIDDEN);
        }

        // Check if OTP has expired
        if (LocalDateTime.now().isAfter(challenge.expiryTime())) {
            otpStorage.remove(key);
            log.debug("OTP expired for email: {}", normalizedEmail);
            return false;
        }

        // Increment attempt count
        OtpChallenge updatedChallenge = challenge.withIncrementedAttempts();

        // Check if OTP matches
        if (challenge.code().equalsIgnoreCase(otp)) {
            otpStorage.remove(key); // Remove after successful verification
            cleanupGenerationAttempts(normalizedEmail); // Reset generation attempts on success
            log.debug("OTP verified successfully for email: {} after {} attempts",
                    normalizedEmail, updatedChallenge.attemptCount());
            return true;
        }

        // Check if max attempts reached
        if (updatedChallenge.attemptCount() >= updatedChallenge.maxAttempts()) {
            updatedChallenge = updatedChallenge.withLocked(true);
            log.warn("OTP challenge locked for email: {} after {} failed attempts",
                    normalizedEmail, updatedChallenge.attemptCount());
        }

        otpStorage.put(key, updatedChallenge);
        log.debug("Invalid OTP provided for email: {}, attempts: {}/{}",
                normalizedEmail, updatedChallenge.attemptCount(), updatedChallenge.maxAttempts());

        return false;
    }

    /**
     * Get remaining attempts for an OTP challenge
     */
    public int getRemainingAttempts(String email) {
        String key = buildKey(email.toLowerCase());
        OtpChallenge challenge = otpStorage.get(key);

        if (challenge == null || challenge.locked() || LocalDateTime.now().isAfter(challenge.expiryTime())) {
            return 0;
        }

        return Math.max(0, challenge.maxAttempts() - challenge.attemptCount());
    }

    /**
     * Check if OTP exists and is valid
     */
    public boolean hasValidOtp(String email) {
        String key = buildKey(email.toLowerCase());
        OtpChallenge challenge = otpStorage.get(key);

        return challenge != null &&
                !challenge.locked() &&
                LocalDateTime.now().isBefore(challenge.expiryTime());
    }

    private boolean isGenerationRateLimited(String email) {
        GenerationAttempts attempts = generationAttempts.get(email);

        if (attempts == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // Clean up old attempts (older than 1 hour)
        if (now.isAfter(attempts.resetTime())) {
            generationAttempts.remove(email);
            return false;
        }

        return attempts.count() >= MAX_GENERATION_ATTEMPTS_PER_HOUR;
    }

    private void updateGenerationAttempts(String email) {
        LocalDateTime now = LocalDateTime.now();
        GenerationAttempts currentAttempts = generationAttempts.get(email);

        if (currentAttempts == null || now.isAfter(currentAttempts.resetTime())) {
            // Create new attempts record
            generationAttempts.put(email, new GenerationAttempts(1, now.plusHours(1)));
        } else {
            // Increment existing attempts
            generationAttempts.put(email,
                    new GenerationAttempts(currentAttempts.count() + 1, currentAttempts.resetTime()));
        }
    }

    private void cleanupGenerationAttempts(String email) {
        generationAttempts.remove(email);
    }

    /**
     * Clean up expired OTPs and generation attempts every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredData() {
        LocalDateTime now = LocalDateTime.now();
        AtomicInteger removedOtps = new AtomicInteger();
        AtomicInteger removedAttempts = new AtomicInteger();

        // Cleanup expired OTPs
        otpStorage.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().expiryTime())) {
                removedOtps.getAndIncrement();
                return true;
            }
            return false;
        });

        // Cleanup expired generation attempts
        generationAttempts.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().resetTime())) {
                removedAttempts.getAndIncrement();
                return true;
            }
            return false;
        });

        if (removedOtps.get() > 0 || removedAttempts.get() > 0) {
            log.debug("Cleaned up {} expired OTP challenges and {} generation attempt records",
                    removedOtps.get(), removedAttempts.get());
        }
    }

    /**
     * Get current OTP storage size (for monitoring)
     */
    public int getStorageSize() {
        return otpStorage.size();
    }

    /**
     * Get generation attempts size (for monitoring)
     */
    public int getGenerationAttemptsSize() {
        return generationAttempts.size();
    }

    private static String buildKey(String email) {
        return "otp:" + email;
    }

    private static String randomToken(int len, Type t) {
        final String digits = "0123456789";
        final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final String alphanum = alpha + digits;

        String chars = switch (t) {
            case NUMERIC -> digits;
            case ALPHA -> alpha;
            case ALPHANUMERIC -> alphanum;
        };

        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * OTP Challenge record with attempt tracking
     */
    private record OtpChallenge(
            String code,
            LocalDateTime expiryTime,
            int attemptCount,
            int maxAttempts,
            LocalDateTime createdAt,
            boolean locked
    ) {
        public OtpChallenge withIncrementedAttempts() {
            return new OtpChallenge(code, expiryTime, attemptCount + 1, maxAttempts, createdAt, locked);
        }

        public OtpChallenge withLocked(boolean locked) {
            return new OtpChallenge(code, expiryTime, attemptCount, maxAttempts, createdAt, locked);
        }
    }

    /**
     * Generation attempts tracking record
     */
    private record GenerationAttempts(
            int count,
            LocalDateTime resetTime
    ) {}
}