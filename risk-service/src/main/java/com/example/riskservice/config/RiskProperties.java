package com.example.riskservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Data
@ConfigurationProperties(prefix = "risk")
public class RiskProperties {
    /** Expected on the order service's inbound risk check call. */
    private String inboundServiceKey = "shared-secret-change-me";
    /** Orders with quantity * price above this are rejected. */
    private BigDecimal notionalLimit = new BigDecimal("1000000");
    private long minLatencyMs = 50;
    private long maxLatencyMs = 200;
}
