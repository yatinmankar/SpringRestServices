package com.example.restarch.persistence;

import com.example.restarch.config.AsyncPersistenceProperties;
import com.example.restarch.downstream.RiskServiceClient;
import com.example.restarch.downstream.ServiceBClient;
import com.example.restarch.entity.Order;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decouples the request path from the DB write path. Requests enqueue an
 * Order and get an immediate response; a small pool of virtual-thread
 * workers drains the queue and writes in batches through Hibernate
 * (OrderBatchWriter), so Envers audits the creation and JDBC batching still
 * applies. Once a batch is durably committed, each order in it is submitted
 * to the risk service and dispatched to Service B - neither happens for a row
 * that didn't actually make it to the DB.
 *
 * Backpressure: if the queue is full, enqueue() returns false and the caller
 * sheds load (503) instead of growing the queue without bound.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPersistenceQueue {

    private final OrderBatchWriter orderBatchWriter;
    private final ServiceBClient serviceBClient;
    private final RiskServiceClient riskServiceClient;
    private final AsyncPersistenceProperties properties;

    private BlockingQueue<Order> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @PostConstruct
    void start() {
        queue = new LinkedBlockingQueue<>(properties.getQueueCapacity());
        for (int i = 0; i < properties.getWorkerThreads(); i++) {
            Thread.ofVirtual().name("order-persist-worker-" + i).start(this::workerLoop);
        }
        log.info("Started {} order persistence workers, queue capacity {}",
                properties.getWorkerThreads(), properties.getQueueCapacity());
    }

    @PreDestroy
    void stop() {
        running.set(false);
    }

    public boolean enqueue(Order order) {
        return queue.offer(order);
    }

    public int queueSize() {
        return queue.size();
    }

    private void workerLoop() {
        List<Order> batch = new ArrayList<>(properties.getBatchSize());
        while (running.get() || !queue.isEmpty()) {
            try {
                Order first = queue.poll(properties.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
                if (first != null) {
                    batch.add(first);
                    queue.drainTo(batch, properties.getBatchSize() - batch.size());
                }
                if (!batch.isEmpty()) {
                    flush(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Batch flush failed, dropping {} records", batch.size(), e);
                batch.clear();
            }
        }
    }

    private void flush(List<Order> batch) {
        try {
            orderBatchWriter.persistBatch(batch);
        } catch (Exception e) {
            log.error("Batch persist failed for {} orders - not dispatching to Service B or the risk service", batch.size(), e);
            return;
        }
        // The risk check is submitted only after the commit: its verdict calls back by
        // correlationId and would otherwise race the insert and find no row.
        for (Order order : batch) {
            riskServiceClient.submitAsync(order);
            serviceBClient.dispatchAsync(order);
        }
    }
}
