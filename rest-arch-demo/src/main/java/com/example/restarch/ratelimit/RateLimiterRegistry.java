package com.example.restarch.ratelimit;

import com.example.restarch.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-client token bucket, in-memory. This enforces the limit correctly for a
 * single instance only. For a multi-instance deployment, back this with
 * bucket4j-redis (or Hazelcast) so the limit is enforced cluster-wide instead
 * of N times (once per instance).
 */
// Explicit bean name: the default ("rateLimiterRegistry") collides with the
// bean Resilience4j's autoconfiguration registers under the same name.
@Component("clientRateLimiterRegistry")
@RequiredArgsConstructor
public class RateLimiterRegistry {

    private final RateLimitProperties properties;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String clientId) {
        return buckets.computeIfAbsent(clientId, id -> newBucket());
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(
                properties.getCapacity(),
                Refill.greedy(properties.getRefillTokens(), Duration.ofSeconds(properties.getRefillDurationSeconds()))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
