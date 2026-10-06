package com.example.restarch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.async")
public class AsyncPersistenceProperties {
    /** Max in-flight orders waiting to be flushed to the DB. Bounds memory and gives backpressure. */
    private int queueCapacity = 50_000;
    /** Number of virtual-thread workers draining the queue. */
    private int workerThreads = 16;
    /** Max rows per batch INSERT. */
    private int batchSize = 200;
    /** Max time a worker waits before flushing a partial batch. */
    private long flushIntervalMs = 50;
}
