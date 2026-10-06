package com.example.restarch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    /** Bucket capacity (max burst) per client. */
    private long capacity = 200;
    /** Tokens added back per refill period. */
    private long refillTokens = 200;
    private long refillDurationSeconds = 1;
}
