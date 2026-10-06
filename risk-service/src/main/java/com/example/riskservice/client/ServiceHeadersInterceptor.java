package com.example.riskservice.client;

import com.example.riskservice.config.OrderServiceProperties;
import com.example.riskservice.correlation.CorrelationContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Applies to every Feign request: shared secret plus the correlation ID from the calling thread's MDC. */
@Component
@RequiredArgsConstructor
public class ServiceHeadersInterceptor implements RequestInterceptor {

    private final OrderServiceProperties properties;

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Service-Key", properties.getOutboundServiceKey());
        String correlationId = CorrelationContext.get();
        if (correlationId != null) {
            template.header(CorrelationContext.HEADER_NAME, correlationId);
        }
    }
}
