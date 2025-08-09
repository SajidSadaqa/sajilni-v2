package com.sajilni.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {
    public OtpService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public enum Type {NUMERIC, ALPHA, ALPHANUMERIC}

    private final StringRedisTemplate redis;

    @Value("${app.otp.length:6}")
    private int length;

    @Value("${app.otp.ttl-minutes:30}")
    private int ttlMinutes;

    @Value("${app.otp.type:NUMERIC}")
    private Type type;

    public String generate(String email) {
        String code = randomToken(length, type);
        String key = key(email);
        redis.opsForValue().set(key, code, Duration.ofMinutes(ttlMinutes));
        return code;
    }

    public boolean verify(String email, String otp) {
        String key = key(email);
        String stored = redis.opsForValue().get(key);
        if (stored != null && stored.equalsIgnoreCase(otp)) {
            redis.delete(key);
            return true;
        }
        return false;
    }

    private static String key(String email) { return "otp:" + email.toLowerCase(); }

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
}
