package com.example.riskservice.client;

import com.example.riskservice.dto.RiskResultCallback;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Delivers a risk verdict to the order service with Retry (outer) around
 * CircuitBreaker (inner); both are configured under resilience4j.*.instances.orderCallback.
 * <p>
 * The fallback is deliberately on @Retry only. A fallback on the circuit breaker would turn
 * every failure into a normal return, and the retry would never see an exception to retry.
 * <p>
 * Known gap: a verdict that exhausts its retries (or hits an open circuit) is only logged -
 * nothing is persisted, so it is lost. Production would write it to an outbox/DLQ here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderResultPublisher {

    private final OrderServiceClient orderServiceClient;

    @Retry(name = "orderCallback", fallbackMethod = "deliveryFailed")
    @CircuitBreaker(name = "orderCallback")
    public void publish(RiskResultCallback result) {
        orderServiceClient.postRiskResult(result);
        log.info("Delivered risk result orderId={} decision={}", result.orderId(), result.decision());
    }

    @SuppressWarnings("unused")
    private void deliveryFailed(RiskResultCallback result, Throwable t) {
        log.error("Giving up delivering risk result orderId={} correlationId={} decision={}: {}",
                result.orderId(), result.correlationId(), result.decision(), t.toString());
    }
}
