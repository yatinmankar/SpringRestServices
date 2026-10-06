package com.example.serviceb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "servicea")
public class ServiceAProperties {
    /** Full URL of Service 1's callback endpoint. */
    private String callbackUrl = "http://localhost:8080/api/v1/orders/callback";
    /** Sent back to Service 1 on the callback call. */
    private String outboundServiceKey = "shared-secret-change-me";
    /** Expected on Service 1's inbound dispatch call to /api/v1/process. */
    private String inboundServiceKey = "shared-secret-change-me";
    private long minLatencyMs = 100;
    private long maxLatencyMs = 400;
    /** Percentage (0-100) of processed orders that resolve as FAILED, to exercise the failure path. */
    private int failurePercentage = 10;
}
