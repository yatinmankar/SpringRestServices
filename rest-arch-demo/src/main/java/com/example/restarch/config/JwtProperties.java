package com.example.restarch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security")
public class JwtProperties {
    private String jwtSecret;
    private long jwtExpiryMs = 3_600_000;
}
