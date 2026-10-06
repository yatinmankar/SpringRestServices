package com.example.riskservice.client;

import com.example.riskservice.dto.RiskResultCallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the order service. Non-2xx responses surface as FeignException
 * (4xx -> FeignClientException, 5xx -> FeignServerException, I/O -> RetryableException),
 * which is what the resilience4j config in application.yml keys off. Feign's own
 * retryer is off by default in Spring Cloud, so retries happen only in
 * {@link OrderResultPublisher}.
 * <p>
 * The X-Service-Key / X-Correlation-Id headers are added by {@link ServiceHeadersInterceptor}.
 */
@FeignClient(name = "order-service", url = "${orderservice.base-url}")
public interface OrderServiceClient {

    @PostMapping("/api/v1/orders/risk-result")
    void postRiskResult(@RequestBody RiskResultCallback result);
}
