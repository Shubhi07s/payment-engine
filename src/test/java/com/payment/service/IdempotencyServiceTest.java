package com.payment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    void lock_shouldReturnTrue_whenKeyDoesNotExist() {
        // Given
        String key = "lock:payment:TXN_123";
        long ttl = 10;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(key), eq("LOCKED"), any(Duration.class)))
                .thenReturn(true);

        // When
        boolean acquired = idempotencyService.lock(key, ttl);

        // Then
        assertTrue(acquired);
    }

    @Test
    void lock_shouldReturnFalse_whenKeyAlreadyExists() {
        // Given
        String key = "lock:payment:TXN_123";
        long ttl = 10;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(key), eq("LOCKED"), any(Duration.class)))
                .thenReturn(false);

        // When
        boolean acquired = idempotencyService.lock(key, ttl);

        // Then
        assertFalse(acquired);
    }
}