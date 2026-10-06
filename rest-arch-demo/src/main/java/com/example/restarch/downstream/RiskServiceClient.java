package com.example.restarch.downstream;

import com.example.restarch.correlation.CorrelationContext;
import com.example.restarch.dto.RiskCheckRequest;
import com.example.restarch.entity.Order;
import com.example.restarch.entity.RiskStatus;
import com.example.restarch.service.RiskResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget: submits the order to the risk service on a virtual thread so
 * the caller (the persistence worker) never waits on it, and does not retry -
 * delivery of the *verdict* is the risk service's job (retry + circuit breaker
 * on its side). If the submit itself fails, the order is marked UNAVAILABLE
 * rather than left PENDING forever.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskServiceClient {

    private final RiskServiceFeignClient feignClient;
    private final RiskResultService riskResultService;

    public void submitAsync(Order order) {
        String correlationId = order.getCorrelationId();
        RiskCheckRequest body = new RiskCheckRequest(
                correlationId, order.getId(), order.getClientId(), order.getInstrumentId(),
                order.getQuantity(), order.getPrice(), order.getSide());

        Thread.ofVirtual().name("risk-submit-" + order.getId()).start(() -> {
            CorrelationContext.set(correlationId);
            try {
                feignClient.submitCheck(body);
            } catch (Exception e) {
                log.warn("Risk check submit failed for correlationId={}: {}", correlationId, e.toString());
                try {
                    riskResultService.apply(correlationId, RiskStatus.UNAVAILABLE, null, "risk service unreachable");
                } catch (Exception inner) {
                    log.error("Could not mark order UNAVAILABLE for correlationId={}", correlationId, inner);
                }
            } finally {
                CorrelationContext.clear();
            }
        });
    }
}
