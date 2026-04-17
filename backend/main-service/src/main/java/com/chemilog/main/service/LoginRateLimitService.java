package com.chemilog.main.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimitService {

    private static final int MAX_FAILURES_PER_MINUTE = 5;
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(1);
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String ipAddress) {
        String key = lockKey(ipAddress);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void registerFailure(String ipAddress) {
        String failKey = failKey(ipAddress);
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, FAIL_WINDOW);
        }
        if (count != null && count >= MAX_FAILURES_PER_MINUTE) {
            redisTemplate.opsForValue().set(lockKey(ipAddress), "1", LOCK_WINDOW);
        }
    }

    public void clearFailures(String ipAddress) {
        redisTemplate.delete(failKey(ipAddress));
    }

    private String failKey(String ipAddress) {
        return "login:fail:" + ipAddress;
    }

    private String lockKey(String ipAddress) {
        return "login:lock:" + ipAddress;
    }
}
