package com.example.restarch.downstream;

import com.example.restarch.config.ServiceBProperties;
import com.example.restarch.correlation.CorrelationContext;
import com.example.restarch.dto.ServiceBDispatchRequest;
import com.example.restarch.entity.Order;
import com.example.restarch.entity.OrderStatus;
import com.example.restarch.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Fires the order at Service B without blocking the caller (WebClient is
 * non-blocking end to end) and, once the dispatch itself succeeds or fails,
 * flips the order's status accordingly. The *outcome* of Service B's actual
 * processing (COMPLETED/FAILED) comes later via the callback endpoint, not
 * from this call - this call only confirms Service B accepted the work.
 *
 * Deliberately lighter-weight than the resilience4j-wrapped result callback in
 * risk-service: a timeout + single retry via Reactor operators, no circuit breaker.
 * Add resilience4j-reactor + CircuitBreakerOperator here if Service B's
 * failure modes turn out to need it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceBClient {

    private final WebClient serviceBWebClient;
    private final ServiceBProperties properties;
    private final OrderRepository orderRepository;

    public void dispatchAsync(Order order) {
        String correlationId = order.getCorrelationId();
        ServiceBDispatchRequest body = new ServiceBDispatchRequest(
                correlationId, order.getId(), order.getInstrumentId(),
                order.getQuantity(), order.getPrice(), order.getSide());

        serviceBWebClient.post()
                .uri(properties.getProcessPath())
                .header(CorrelationContext.HEADER_NAME, correlationId)
                .header("X-Service-Key", properties.getOutboundServiceKey())
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .retry(1)
                .subscribe(
                        response -> markSentToServiceB(order.getId()),
                        error -> {
                            log.warn("Dispatch to Service B failed for correlationId={}: {}", correlationId, error.toString());
                            markDispatchFailed(order.getId());
                        }
                );
    }

    private void markSentToServiceB(String orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            if (o.getStatus() == OrderStatus.CREATED) {
                o.setStatus(OrderStatus.SENT_TO_SERVICE_B);
                orderRepository.save(o);
            }
        });
    }

    private void markDispatchFailed(String orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus(OrderStatus.FAILED);
            orderRepository.save(o);
        });
    }
}
