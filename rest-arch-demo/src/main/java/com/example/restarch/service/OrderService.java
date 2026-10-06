package com.example.restarch.service;

import com.example.restarch.correlation.CorrelationContext;
import com.example.restarch.dto.OrderRequest;
import com.example.restarch.dto.OrderResponse;
import com.example.restarch.dto.OrderUpdateRequest;
import com.example.restarch.entity.IdempotencyRecord;
import com.example.restarch.entity.Order;
import com.example.restarch.entity.OrderStatus;
import com.example.restarch.exception.OrderNotFoundException;
import com.example.restarch.exception.OrderStateException;
import com.example.restarch.exception.ServiceOverloadedException;
import com.example.restarch.persistence.OrderPersistenceQueue;
import com.example.restarch.repository.IdempotencyRepository;
import com.example.restarch.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final Cache<String, OrderResponse> idempotencyCache;
    private final IdempotencyRepository idempotencyRepository;
    private final OrderRepository orderRepository;
    private final OrderPersistenceQueue persistenceQueue;
    private final ObjectMapper objectMapper;

    public OrderResponse processOrder(String clientId, String idempotencyKey, OrderRequest request) {

        String correlationId = CorrelationContext.get();

        OrderResponse cached = idempotencyCache.getIfPresent(idempotencyKey);
        if (cached != null) {
            // Replays the ORIGINAL request's outcome, including its original correlationId -
            // not the retry's - since that's the request that actually ran.
            return cached.withReplay(true);
        }

        if (!reserve(idempotencyKey, clientId)) {
            return awaitExistingResult(idempotencyKey);
        }

        String orderId = UUID.randomUUID().toString();
        Order order = Order.builder()
                .id(orderId)
                .clientId(clientId)
                .correlationId(correlationId)
                .instrumentId(request.getInstrumentId())
                .quantity(request.getQuantity())
                .price(BigDecimal.valueOf(request.getPrice()))
                .side(request.getSide().name())
                .status(OrderStatus.CREATED)
                .build();

        if (!persistenceQueue.enqueue(order)) {
            releaseReservation(idempotencyKey);
            throw new ServiceOverloadedException("persistence queue is full, retry later");
        }

        // The risk check is submitted to the risk service after the order is persisted (see
        // OrderPersistenceQueue) and its verdict lands on the order later, as does the
        // CREATED -> SENT_TO_SERVICE_B -> COMPLETED/FAILED lifecycle. Neither is known here.
        OrderResponse response = new OrderResponse(
                orderId, correlationId, "ACCEPTED", "order accepted; risk check pending", false);
        idempotencyCache.put(idempotencyKey, response);
        completeReservation(idempotencyKey, response);

        return response;
    }

    @Transactional(readOnly = true)
    public Order getOrder(String clientId, String orderId) {
        return orderRepository.findByIdAndClientId(orderId, clientId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders(String clientId) {
        return orderRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrders(String clientId, Pageable pageable) {
        return orderRepository.findByClientId(clientId, pageable);
    }

    /** Only orders not yet dispatched to Service B (status CREATED) may be amended. */
    @Transactional
    public Order updateOrder(String clientId, String orderId, OrderUpdateRequest request) {
        Order order = getOrder(clientId, orderId);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new OrderStateException(
                    "order " + orderId + " is " + order.getStatus() + " and can no longer be modified");
        }
        order.setQuantity(request.getQuantity());
        order.setPrice(BigDecimal.valueOf(request.getPrice()));
        return orderRepository.save(order);
    }

    /** Called from OrderCallbackController when Service B reports a final outcome. */
    @Transactional
    public void applyServiceBCallback(String correlationId, OrderStatus resultStatus, String message) {
        Optional<Order> maybeOrder = orderRepository.findByCorrelationId(correlationId);
        if (maybeOrder.isEmpty()) {
            log.warn("Callback for unknown correlationId={}", correlationId);
            return;
        }
        Order order = maybeOrder.get();
        if (order.getStatus() != OrderStatus.SENT_TO_SERVICE_B) {
            log.warn("Ignoring callback for correlationId={} - order not awaiting one (current status={})",
                    correlationId, order.getStatus());
            return;
        }
        order.setStatus(resultStatus);
        orderRepository.save(order);
        log.info("Order correlationId={} orderId={} -> {} ({})",
                correlationId, order.getId(), resultStatus, message);
    }

    /** Insert a PROCESSING placeholder row; the unique PK acts as a lock on the key across all instances. */
    private boolean reserve(String idempotencyKey, String clientId) {
        try {
            idempotencyRepository.saveAndFlush(
                    new IdempotencyRecord(idempotencyKey, clientId, "PROCESSING", null, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional
    protected void completeReservation(String idempotencyKey, OrderResponse response) {
        try {
            String body = objectMapper.writeValueAsString(response);
            idempotencyRepository.findById(idempotencyKey).ifPresent(rec -> {
                rec.setResponseStatus("COMPLETED");
                rec.setResponseBody(body);
                idempotencyRepository.save(rec);
            });
        } catch (Exception e) {
            log.error("Failed to persist idempotency response for key {}", idempotencyKey, e);
        }
    }

    private void releaseReservation(String idempotencyKey) {
        idempotencyRepository.deleteById(idempotencyKey);
    }

    /** A concurrent duplicate lost the race to reserve the key; poll briefly for the winner's result. */
    private OrderResponse awaitExistingResult(String idempotencyKey) {
        for (int attempt = 0; attempt < 10; attempt++) {
            Optional<IdempotencyRecord> record = idempotencyRepository.findById(idempotencyKey);
            if (record.isPresent() && "COMPLETED".equals(record.get().getResponseStatus())) {
                try {
                    OrderResponse response = objectMapper.readValue(record.get().getResponseBody(), OrderResponse.class);
                    idempotencyCache.put(idempotencyKey, response);
                    return response.withReplay(true);
                } catch (Exception e) {
                    break;
                }
            }
            sleep(20);
        }
        return new OrderResponse(null, CorrelationContext.get(), "PROCESSING",
                "duplicate request is still being processed, retry shortly", true);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
