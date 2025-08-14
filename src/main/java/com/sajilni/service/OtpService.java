package com.sajilni.service;

import com.sajilni.domain.constants.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Todo : put otp table in database
@Slf4j
@Service
public class OtpService {
    // In-memory storage for OTP codes
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final ConcurrentHashMap<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    @Value("${app.otp.length:6}")
    private int length;

    @Value("${app.otp.ttl-minutes:30}")
    private int ttlMinutes;

    @Value("${app.otp.type:NUMERIC}")
    private Type type;

    /**
     * Generate OTP and store it in memory
     */
    public String genrate(String email) { // Keeping original method name for compatibility
        return generate(email);
    }

    public String generate(String email) {
        String code = randomToken(length, type);
        String key = key(email);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(ttlMinutes);

        otpStorage.put(key, new OtpData(code, expiryTime));
        log.debug("OTP generated for {}, expires at {}", email, expiryTime);

        return code;
    }

    /**
     * Verify OTP and remove it if valid
     */

    // we should use challenge here
    public boolean verify(String email, String otp) {
        String key = key(email);
        OtpData otpData = otpStorage.get(key);

        if (otpData == null) {
            log.debug("No OTP found for email: {}", email);
            return false;
        }

        // Check if OTP has expired
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpStorage.remove(key);
            log.debug("OTP expired for email: {}", email);
            return false;
        }

        // Check if OTP matches
        if (otpData.code.equalsIgnoreCase(otp)) {
            otpStorage.remove(key); // Remove after successful verification
            log.debug("OTP verified successfully for email: {}", email);
            return true;
        }

        log.debug("Invalid OTP provided for email: {}", email);
        return false;
    }

    /**
     * Clean up expired OTPs every 5 minutes
     * TODO: In production, consider using a proper cache with TTL like Caffeine
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        AtomicInteger removedCount = new AtomicInteger();

        otpStorage.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().expiryTime)) {
                removedCount.getAndIncrement();
                return true;
            }
            return false;
        });

        if (removedCount.get() > 0) {
            log.debug("Cleaned up {} expired OTP entries", removedCount);
        }
    }

    /**
     * Get current OTP storage size (for monitoring)
     */
    public int getStorageSize() {
        return otpStorage.size();
    }

    private static String key(String email) {
        return "otp:" + email.toLowerCase();
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
     * Inner class to store OTP with expiry time
     */
    private record OtpData(String code, LocalDateTime expiryTime) {}
}