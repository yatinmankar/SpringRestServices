package com.example.restarch.config;

import com.example.restarch.dto.OrderResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    /**
     * Local, per-instance cache of completed responses keyed by Idempotency-Key.
     * Fast path for duplicate-request detection. The DB unique-constraint on
     * IdempotencyRecord is the source of truth across instances/restarts; this
     * cache just avoids a DB round trip on the common case.
     */
    @Bean
    public Cache<String, OrderResponse> idempotencyCache() {
        return Caffeine.newBuilder()
                .maximumSize(500_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
