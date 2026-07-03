package com.comandago.api.shared.config;

import com.comandago.api.shared.exception.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private final RateLimitProperties properties;
    private Cache<String, AtomicInteger> attempts;

    @PostConstruct
    void init() {
        attempts = Caffeine.newBuilder()
                .expireAfterWrite(properties.getWindowSeconds(), TimeUnit.SECONDS)
                .maximumSize(10_000)
                .build();
    }

    public void checkAllowed(String clientKey) {
        AtomicInteger counter = attempts.getIfPresent(clientKey);
        if (counter != null && counter.get() >= properties.getMaxAttempts()) {
            throw new TooManyRequestsException("Demasiados intentos. Intente más tarde.");
        }
    }

    public void recordFailedAttempt(String clientKey) {
        AtomicInteger counter = attempts.get(clientKey, key -> new AtomicInteger(0));
        counter.incrementAndGet();
    }

    public void reset(String clientKey) {
        attempts.invalidate(clientKey);
    }
}
