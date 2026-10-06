package com.example.serviceb.service;

import com.example.serviceb.config.ServiceAProperties;
import com.example.serviceb.correlation.CorrelationContext;
import com.example.serviceb.dto.CallbackRequest;
import com.example.serviceb.dto.ProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Returns to the controller immediately; the actual "work" and the callback
 * to Service 1 both happen on a separate virtual thread. Blocking on
 * .block() there is intentional and cheap - virtual threads are built for
 * exactly this - rather than chaining everything through Reactor across the
 * thread handoff.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final WebClient serviceAWebClient;
    private final ServiceAProperties properties;

    public void processAsync(ProcessRequest request, String correlationId) {
        log.info("Processing Request for correlationId={} ",correlationId);

        Thread.ofVirtual().name("process-" + request.getOrderId()).start(() -> {
            CorrelationContext.set(correlationId);
            try {
                doWork(request);
            } finally {
                CorrelationContext.clear();
            }
        });
    }

    private void doWork(ProcessRequest request) {
        simulateLatency();

        boolean success = ThreadLocalRandom.current().nextInt(100) >= properties.getFailurePercentage();
        String status = success ? "COMPLETED" : "FAILED";
        String message = success ? "processed ok" : "simulated processing failure";

        callBack(new CallbackRequest(request.getCorrelationId(), status, message));
    }

    private void callBack(CallbackRequest callback) {
        try {
            serviceAWebClient.post()
                    .uri(properties.getCallbackUrl())
                    .header(CorrelationContext.HEADER_NAME, callback.correlationId())
                    .header("X-Service-Key", properties.getOutboundServiceKey())
                    .bodyValue(callback)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(2))
                    .block();
        } catch (Exception e) {
            log.error("Callback to Service 1 failed for correlationId={}: {}", callback.correlationId(), e.toString());
        }
    }

    private void simulateLatency() {
        long delay = ThreadLocalRandom.current().nextLong(properties.getMinLatencyMs(), properties.getMaxLatencyMs());
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
