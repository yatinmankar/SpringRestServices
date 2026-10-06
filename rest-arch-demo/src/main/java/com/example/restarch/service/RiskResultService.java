package com.example.restarch.service;

import com.example.restarch.entity.Order;
import com.example.restarch.entity.RiskStatus;
import com.example.restarch.exception.OrderNotFoundException;
import com.example.restarch.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Records a risk verdict on the order. Kept separate from OrderService because
 * OrderService -> OrderPersistenceQueue -> RiskServiceClient already exists and
 * RiskServiceClient needs this too (it would otherwise be a constructor cycle).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskResultService {

    private static final int MAX_ATTEMPTS = 3;

    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Idempotent: only a PENDING order is updated, so a retried callback is a no-op.
     * The order row is also written by Service B's dispatch/callback; on an optimistic-lock
     * clash the whole read-modify-write is redone, since each attempt needs a fresh read.
     *
     * @throws OrderNotFoundException if no order has this correlation ID
     */
    public void apply(String correlationId, RiskStatus status, Integer score, String message) {
        for (int attempt = 1; ; attempt++) {
            try {
                transactionTemplate.executeWithoutResult(tx -> update(correlationId, status, score, message));
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                log.debug("Optimistic lock clash applying risk result for correlationId={}, attempt {}", correlationId, attempt);
            }
        }
    }

    private void update(String correlationId, RiskStatus status, Integer score, String message) {
        Order order = orderRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new OrderNotFoundException(correlationId));
        if (order.getRiskStatus() != RiskStatus.PENDING) {
            log.warn("Ignoring risk result for correlationId={} - order risk status is {}",
                    correlationId, order.getRiskStatus());
            return;
        }
        order.setRiskStatus(status);
        order.setRiskScore(score);
        order.setRiskMessage(message);
        orderRepository.save(order);
        log.info("Order correlationId={} orderId={} risk -> {} score={} ({})",
                correlationId, order.getId(), status, score, message);
    }
}
