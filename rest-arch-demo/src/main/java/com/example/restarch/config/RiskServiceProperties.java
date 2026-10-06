package com.example.restarch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "riskservice")
public class RiskServiceProperties {
    private String baseUrl = "http://localhost:8082";
    /** Sent to the risk service on the outbound check call. */
    private String outboundServiceKey = "shared-secret-change-me";
    /** Expected on the risk service's inbound result callback to this service. */
    private String inboundServiceKey = "shared-secret-change-me";
}
