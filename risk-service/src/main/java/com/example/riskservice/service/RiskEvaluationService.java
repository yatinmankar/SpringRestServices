package com.example.riskservice.service;

import com.example.riskservice.client.OrderResultPublisher;
import com.example.riskservice.config.RiskProperties;
import com.example.riskservice.correlation.CorrelationContext;
import com.example.riskservice.dto.RiskCheckRequest;
import com.example.riskservice.dto.RiskResultCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Evaluates off the request thread (a virtual thread, so blocking in the Feign
 * callback and its retry back-off is cheap) and hands the verdict to
 * {@link OrderResultPublisher}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEvaluationService {

    private final RiskProperties properties;
    private final OrderResultPublisher publisher;

    public void evaluateAsync(RiskCheckRequest request, String correlationId) {
        Thread.ofVirtual().name("risk-" + request.getOrderId()).start(() -> {
            CorrelationContext.set(correlationId);
            try {
                simulateLatency();
                publisher.publish(evaluate(request));
            } catch (Exception e) {
                log.error("Risk evaluation failed for orderId={}", request.getOrderId(), e);
            } finally {
                CorrelationContext.clear();
            }
        });
    }

    /** Single rule for the demo: notional (quantity * price) against a configured limit. */
    RiskResultCallback evaluate(RiskCheckRequest request) {
        BigDecimal notional = BigDecimal.valueOf(request.getPrice()).multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal limit = properties.getNotionalLimit();

        int score = notional.multiply(BigDecimal.valueOf(100))
                .divide(limit, 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100))
                .intValue();

        boolean approved = notional.compareTo(limit) <= 0;
        String message = approved
                ? "notional " + notional.toPlainString() + " within limit"
                : "notional " + notional.toPlainString() + " exceeds limit " + limit.toPlainString();

        log.info("Risk verdict orderId={} {} score={}", request.getOrderId(), approved ? "APPROVED" : "REJECTED", score);
        return new RiskResultCallback(request.getCorrelationId(), request.getOrderId(),
                approved ? "APPROVED" : "REJECTED", score, message);
    }

    private void simulateLatency() {
        long min = properties.getMinLatencyMs();
        long max = Math.max(properties.getMaxLatencyMs(), min + 1);
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(min, max));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
