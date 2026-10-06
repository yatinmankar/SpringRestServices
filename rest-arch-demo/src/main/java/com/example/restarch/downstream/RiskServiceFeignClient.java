package com.example.restarch.downstream;

import com.example.restarch.dto.RiskCheckRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the risk service. The call returns 202 with no body; the
 * verdict comes back later on POST /api/v1/orders/risk-result. Timeouts live
 * under spring.cloud.openfeign.client.config.risk-service in application.yml.
 */
@FeignClient(name = "risk-service", url = "${riskservice.base-url}")
public interface RiskServiceFeignClient {

    @PostMapping("/api/v1/risk/check")
    void submitCheck(@RequestBody RiskCheckRequest request);
}
