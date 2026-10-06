package com.example.riskservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "orderservice")
public class OrderServiceProperties {
    private String baseUrl = "http://localhost:8080";
    /** Sent to the order service on the result callback. */
    private String outboundServiceKey = "shared-secret-change-me";
}
