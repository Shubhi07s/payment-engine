package com.payment.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Tries to acquire a distributed lock in Redis for the given key.
     * @param key The idempotency key (e.g., "lock:payment:TXN_123")
     * @param ttlSeconds How long the lock remains valid
     * @return true if lock was acquired (first request), false if lock already exists (duplicate)
     */
    public boolean lock(String key, long ttlSeconds) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", Duration.ofSeconds(ttlSeconds));

        // Handle potential null response safely
        return Boolean.TRUE.equals(success);
    }

    /**
     * Releases the lock manually if needed (e.g., on processing failure).
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}