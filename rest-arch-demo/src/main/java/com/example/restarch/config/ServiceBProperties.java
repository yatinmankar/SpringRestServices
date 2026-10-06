package com.example.restarch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "serviceb")
public class ServiceBProperties {
    private String baseUrl = "http://localhost:8081";
    private String processPath = "/api/v1/process";
    /** Sent to Service B on the outbound dispatch call. */
    private String outboundServiceKey = "shared-secret-change-me";
    /** Expected on Service B's inbound callback to this service. */
    private String inboundServiceKey = "shared-secret-change-me";
    private long timeoutMs = 800;
}
