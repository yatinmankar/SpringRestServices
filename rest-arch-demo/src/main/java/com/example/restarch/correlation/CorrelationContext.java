package com.example.restarch.correlation;

import org.slf4j.MDC;

/**
 * A correlation ID is generated (or honored, if the caller already supplied
 * one) at the edge of Service 1, kept on the logging MDC for the life of the
 * request/async task, persisted on the order row, and forwarded as a header
 * to Service B so log lines and status callbacks can be joined across both
 * services.
 */
public final class CorrelationContext {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationContext() {
    }

    public static void set(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    public static String get() {
        return MDC.get(MDC_KEY);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
